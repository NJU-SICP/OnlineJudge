package cn.edu.nju.sicp.listeners;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Result;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.rabbitmq.client.Channel;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.StopWatch;
import org.bson.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class GradeSubmissionListener implements ChannelAwareMessageListener {

    private final S3Config s3Config;
    private final DockerConfig dockerConfig;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final Logger logger;

    public GradeSubmissionListener(S3Config s3Config, DockerConfig dockerConfig,
            AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository) {
        this.s3Config = s3Config;
        this.dockerConfig = dockerConfig;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.logger = LoggerFactory.getLogger(BuildImageListener.class);
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        logger.debug(String.format("Receive AMQP %s", message));
        String submissionId = new String(message.getBody());
        try {
            Submission submission = submissionRepository.findById(submissionId).orElseThrow();
            Assignment assignment = assignmentRepository
                    .findById(submission.getAssignmentId()).orElseThrow();
            gradeSubmission(assignment, submission);
        } catch (NoSuchElementException ignored) {
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    private void gradeSubmission(Assignment assignment, Submission submission) {
        S3Client s3 = s3Config.getInstance();
        String s3Bucket = s3Config.getBucket();
        DockerClient docker = dockerConfig.getInstance();

        Grader grader = assignment.getGrader();
        if (grader == null) {
            Result result = new Result();
            result.setMessage("此作业未配置自动测试，请等待管理员手动评分。");
            submission.setResult(result);
            submissionRepository.save(submission);
            return;
        }

        String imageId = grader.getImageId();
        if (imageId == null) {
            Calendar retry = Calendar.getInstance();
            retry.add(Calendar.MINUTE, 5);

            Result result = new Result();
            result.setError("管理员配置了自动测试，但测评镜像尚未编译完成或编译失败，请联系管理员或重新提交（OJ暂不支持自动重新测试）。");
            result.setRetryAt(retry.getTime());
            result.setGradedAt(new Date());
            submission.setResult(result);
            submissionRepository.save(submission);
            return;
        }

        logger.info(String.format("GradeSubmission start: %s", submission));
        StringBuilder logBuilder = new StringBuilder();
        StopWatch stopWatch = new StopWatch();
        Consumer<String> logStopWatch = (message) -> {
            logBuilder.append(
                    String.format("%05.2fs %s", (double) stopWatch.getTime() / 1000, message));
        };

        try {
            stopWatch.start();
            InspectImageResponse inspect = docker.inspectImageCmd(imageId).exec();
            ContainerConfig config = inspect.getConfig();
            List<String> args =
                    new ArrayList<>(List.of("/usr/bin/python3", "ok", "--score", "--score-out"));
            if (config != null && config.getEntrypoint() != null) {
                args = new ArrayList<>(List.of(config.getEntrypoint()));
            }
            args.add(String.format("%s.json", UUID.randomUUID()));
            String containerId = docker.createContainerCmd(imageId)
                    .withEntrypoint(args.toArray(new String[0]))
                    .withNetworkDisabled(true)
                    .withHostConfig(new HostConfig()
                            .withCpuPeriod(100000L)
                            .withCpuQuota(100000L)
                            .withMemory(256L * 1024 * 1024)
                            .withMemorySwappiness(0L))
                    .exec()
                    .getId();
            logStopWatch.accept(String.format("Created Docker container.\n   ID: %s\n   RF: %s\n",
                    containerId, args.get(args.size() - 1)));

            Path temp = Files.createTempFile("grader-tar", ".tar");
            Path json = Files.createTempFile("grader-json", ".json");
            String key = submission.getKey();
            try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
                    ArchiveOutputStream tarOutputStream =
                            new TarArchiveOutputStream(tempOutputStream);
                    InputStream s3Stream =
                            s3.getObject(builder -> builder.bucket(s3Bucket).key(key).build())) {
                if (Objects.equals(assignment.getSubmitFileType(), ".zip")) {
                    // zip archive
                    try (ZipInputStream zipInputStream = new ZipInputStream(s3Stream)) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                            TarArchiveEntry archiveEntry = new TarArchiveEntry(zipEntry.getName());
                            archiveEntry.setSize(zipEntry.getSize());
                            tarOutputStream.putArchiveEntry(archiveEntry);
                            IOUtils.copy(zipInputStream, tarOutputStream);
                            tarOutputStream.closeArchiveEntry();
                        }
                    }
                } else {
                    // single file (.pdf or .py)
                    String name = assignment.getSubmitFileName() + assignment.getSubmitFileType();
                    TarArchiveEntry entry = new TarArchiveEntry(name);
                    entry.setSize(s3Stream.available());
                    tarOutputStream.putArchiveEntry(entry);
                    IOUtils.copy(s3Stream, tarOutputStream);
                    tarOutputStream.closeArchiveEntry();
                }
            }
            logStopWatch.accept("Submit file copied to container.\n");

            docker.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(Files.newInputStream(temp)).withRemotePath("/workdir")
                    .exec();
            docker.startContainerCmd(containerId).exec();
            logStopWatch.accept("Docker container started.\n");
            if (!docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())
                    .awaitCompletion(60, TimeUnit.SECONDS)) {
                logStopWatch.accept("Docker container timeout.\n");
                throw new Exception("判题容器执行程序超时。");
            }
            logBuilder.append(String.format("%05.2fs Docker container stopped.\n",
                    (double) stopWatch.getTime() / 1000));

            try (InputStream resultStream = docker.copyArchiveFromContainerCmd(containerId,
                    String.format("/workdir/%s", args.get(args.size() - 1))).exec();
                    TarArchiveInputStream tarInputStream =
                            new TarArchiveInputStream(resultStream)) {
                logBuilder.append(String.format("%05.2fs Result file retrieved from container.\n",
                        (double) stopWatch.getTime() / 1000));
                docker.removeContainerCmd(containerId).exec(); // remove container after getting
                                                               // data
                logBuilder.append(String.format("%05.2fs Docker container removed.\n",
                        (double) stopWatch.getTime() / 1000));
                ArchiveEntry entry = tarInputStream.getNextEntry();
                if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                    logger.error(String.format(
                            "GradeSubmission failed: result json does not exist %s", assignment));
                    throw new Exception("进行测试后测试程序没有保存结果或保存的结果已损坏。");
                } else {
                    try (OutputStream jsonStream = Files.newOutputStream(json)) {
                        IOUtils.copy(tarInputStream, jsonStream);
                    }
                }
            }
            Result result = (new ObjectMapper()).readValue(json.toFile(), Result.class);
            logBuilder.append(String.format("%05.2fs Grade process complete.\n",
                    (double) stopWatch.getTime() / 1000));
            result.setLog(logBuilder.toString());
            if (result.getScore() == null) {
                result.setScore(result.getDetails().stream()
                        .mapToInt(Result.ScoreDetail::getScore).sum());
            }
            result.setGradedAt(new Date());
            submission.setGraded(true);
            submission.setResult(result);
        } catch (JsonParseException | JsonMappingException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(),
                    e.getMessage()));
            result.setError("判题程序的返回结果无法阅读。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (IOException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(),
                    e.getMessage()));
            result.setError("判题程序执行中遇到IO错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (DockerClientException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(),
                    e.getMessage()));
            result.setError("判题程序执行中遇到Docker错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (Exception e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(),
                    e.getMessage()));
            result.setError(e.getMessage());
            result.setGradedAt(new Date());
            submission.setResult(result);
        } finally {
            submissionRepository.save(submission);
            stopWatch.stop();
            logger.info(String.format("GradeSubmission finish: elapsed=%05.2fs %s",
                    (double) stopWatch.getTime() / 1000, submission));
        }
    }

}
