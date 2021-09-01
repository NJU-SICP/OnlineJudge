package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Result;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
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
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GradeSubmissionTask implements Runnable, Comparable<GradeSubmissionTask> {

    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORM = 0;
    public static final int PRIORITY_LOW = -1;

    private final int priority;
    private final Assignment assignment;
    private final Submission submission;
    private final SubmissionRepository repository;
    private final String s3Bucket;
    private final S3Client s3Client;
    private final DockerClient client;
    private final Logger logger;

    public GradeSubmissionTask(Assignment assignment,
                               Submission submission,
                               SubmissionRepository repository,
                               String s3Bucket,
                               S3Client s3Client,
                               DockerClient client) {
        this(assignment, submission, repository, s3Bucket, s3Client, client, PRIORITY_NORM);
    }

    public GradeSubmissionTask(Assignment assignment,
                               Submission submission,
                               SubmissionRepository repository,
                               String s3Bucket,
                               S3Client s3Client,
                               DockerClient client,
                               int priority) {
        this.assignment = assignment;
        this.submission = submission;
        this.repository = repository;
        this.priority = priority;
        this.s3Bucket = s3Bucket;
        this.s3Client = s3Client;
        this.client = client;
        this.logger = LoggerFactory.getLogger(GradeSubmissionTask.class);
    }

    @Override
    public void run() {
        Grader grader = assignment.getGrader();
        if (grader == null) {
            Result result = new Result();
            result.setMessage("此作业未配置自动测试，请等待管理员手动评分。");
            submission.setResult(result);
            repository.save(submission);
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
            repository.save(submission);
            return;
        }

        logger.info(String.format("GradeSubmission start: %s", submission));
        StringBuilder logBuilder = new StringBuilder();
        StopWatch stopWatch = new StopWatch();
        try {
            stopWatch.start();
            InspectImageResponse inspect = client.inspectImageCmd(imageId).exec();
            ContainerConfig config = inspect.getConfig();
            List<String> args = new ArrayList<>(List.of("/usr/bin/python3", "ok", "--score", "--score-out"));
            if (config != null && config.getEntrypoint() != null) {
                args = new ArrayList<>(List.of(config.getEntrypoint()));
            }
            args.add(String.format("%s.json", UUID.randomUUID()));
            String containerId = client.createContainerCmd(imageId)
                    .withEntrypoint(args.toArray(new String[0]))
                    .withNetworkDisabled(true)
                    .withHostConfig(new HostConfig()
                            .withCpuPeriod(100000L)
                            .withCpuQuota(100000L)
                            .withMemory(256L * 1024 * 1024)
                            .withMemorySwappiness(0L))
                    .exec().getId();
            logBuilder.append(String.format("%05.2fs Created Docker container.\n   ID: %s\n   RF: %s\n",
                    (double) stopWatch.getTime() / 1000, containerId, args.get(args.size() - 1)));

            Path temp = Files.createTempFile("grader-tar", ".tar");
            Path json = Files.createTempFile("grader-json", ".json");
            String key = submission.getKey();
            try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
                 ArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tempOutputStream);
                 InputStream s3Stream = s3Client.getObject(builder -> builder.bucket(s3Bucket).key(key).build())) {
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
            logBuilder.append(String.format("%05.2fs Submit file copied to container.\n", (double) stopWatch.getTime() / 1000));

            client.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(Files.newInputStream(temp))
                    .withRemotePath("/workdir")
                    .exec();
            client.startContainerCmd(containerId).exec();
            logBuilder.append(String.format("%05.2fs Docker container started.\n", (double) stopWatch.getTime() / 1000));
            if (!client.waitContainerCmd(containerId)
                    .exec(new WaitContainerResultCallback())
                    .awaitCompletion(60, TimeUnit.SECONDS)) {
                logBuilder.append(String.format("%05.2fs Docker container timeout.\n", (double) stopWatch.getTime() / 1000));
                throw new Exception("判题容器执行程序超时。");
            }
            logBuilder.append(String.format("%05.2fs Docker container stopped.\n", (double) stopWatch.getTime() / 1000));

            try (InputStream resultStream = client.copyArchiveFromContainerCmd(containerId, String.format("/workdir/%s", args.get(args.size() - 1))).exec();
                 TarArchiveInputStream tarInputStream = new TarArchiveInputStream(resultStream)) {
                logBuilder.append(String.format("%05.2fs Result file retrieved from container.\n", (double) stopWatch.getTime() / 1000));
                client.removeContainerCmd(containerId).exec(); // remove container after getting data
                logBuilder.append(String.format("%05.2fs Docker container removed.\n", (double) stopWatch.getTime() / 1000));
                ArchiveEntry entry = tarInputStream.getNextEntry();
                if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                    logger.error(String.format("GradeSubmission failed: result json does not exist %s", assignment));
                    throw new Exception("进行测试后测试程序没有保存结果或保存的结果已损坏。");
                } else {
                    try (OutputStream jsonStream = Files.newOutputStream(json)) {
                        IOUtils.copy(tarInputStream, jsonStream);
                    }
                }
            }
            Result result = (new ObjectMapper()).readValue(json.toFile(), Result.class);
            logBuilder.append(String.format("%05.2fs Grade process complete.\n", (double) stopWatch.getTime() / 1000));
            result.setLog(logBuilder.toString());
            if (result.getScore() == null) {
                result.setScore(result.getDetails().stream().mapToInt(Result.ScoreDetail::getScore).sum());
            }
            if (result.getGradedAt() == null) {
                result.setGradedAt(new Date());
            }
            submission.setGraded(true);
            submission.setResult(result);
        } catch (JsonParseException | JsonMappingException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(), e.getMessage()));
            result.setError("判题程序的返回结果无法阅读。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (IOException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(), e.getMessage()));
            result.setError("判题程序执行中遇到IO错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (DockerClientException e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(), e.getMessage()));
            result.setError("判题程序执行中遇到Docker错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (Exception e) {
            Result result = new Result();
            result.setLog(String.format("%s\n\n%s: %s", logBuilder, e.getClass().getName(), e.getMessage()));
            result.setError(e.getMessage());
            result.setGradedAt(new Date());
            submission.setResult(result);
        } finally {
            repository.save(submission);
            stopWatch.stop();
            logger.info(String.format("GradeSubmission finish: elapsed=%05.2fs %s",
                    (double) stopWatch.getTime() / 1000, submission));
        }
    }

    @Override
    public int compareTo(GradeSubmissionTask o) {
        return this.priority - o.priority;
    }

}
