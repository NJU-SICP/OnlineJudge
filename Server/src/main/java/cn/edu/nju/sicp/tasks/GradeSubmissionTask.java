package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Result;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.bson.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

public class GradeSubmissionTask implements Runnable, Comparable<GradeSubmissionTask> {

    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORM = 0;
    public static final int PRIORITY_LOW = -1;

    private final int priority;
    private final Assignment assignment;
    private final Submission submission;
    private final SubmissionRepository repository;
    private final Logger logger;

    public GradeSubmissionTask(Assignment assignment, Submission submission, SubmissionRepository repository) {
        this(assignment, submission, repository, PRIORITY_NORM);
    }

    public GradeSubmissionTask(Assignment assignment, Submission submission, SubmissionRepository repository, int priority) {
        this.assignment = assignment;
        this.submission = submission;
        this.repository = repository;
        this.priority = priority;
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
            result.setError("管理员配置了自动测试，但测评镜像尚未编译完成或编译失败，稍后将重新测试。");
            result.setRetryAt(retry.getTime());
            result.setGradedAt(new Date());
            submission.setResult(result);
            repository.save(submission);
            return;
        }

        try {
            DockerClient client = DockerConfig.getInstance();
            String containerId = client.createContainerCmd(imageId).exec().getId();

            Path temp = Files.createTempFile("grader-tar", ".tar");
            Path json = Files.createTempFile("grader-json", ".json");
            Path file = Paths.get(submission.getFilePath());
            try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
                 ArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tempOutputStream);) {
                ArchiveEntry entry = tarOutputStream.createArchiveEntry(file.toFile(), file.getFileName().toString());
                tarOutputStream.putArchiveEntry(entry);
                try (InputStream inputStream = Files.newInputStream(file)) {
                    IOUtils.copy(inputStream, tarOutputStream);
                }
                tarOutputStream.closeArchiveEntry();
            }

            client.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(Files.newInputStream(temp))
                    .withRemotePath("/workdir")
                    .exec();
            client.startContainerCmd(containerId).exec();
            client.waitContainerCmd(containerId).exec(new WaitContainerResultCallback()).awaitStatusCode();
            try (InputStream resultStream = client.copyArchiveFromContainerCmd(containerId, "/workdir/result.json").exec();
                 TarArchiveInputStream tarInputStream = new TarArchiveInputStream(resultStream)) {
                ArchiveEntry entry = tarInputStream.getNextEntry();
                if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                    logger.error(String.format("GradeSubmission failed: result.json does not exist %s", assignment));
                    throw new Exception("进行测试后测试程序没有保存结果或保存的结果已损坏。");
                } else {
                    logger.info("Entry: " + entry.getName());
                    try (OutputStream jsonStream = Files.newOutputStream(json)) {
                        IOUtils.copy(tarInputStream, jsonStream);
                    }
                }
            }
            Result result = (new ObjectMapper()).readValue(json.toFile(), Result.class);
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
            result.setLog(e.getClass().getName() + ": " + e.getMessage());
            result.setError("判题程序的返回结果无法阅读。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (IOException e) {
            Result result = new Result();
            result.setLog(e.getClass().getName() + ": " + e.getMessage());
            result.setError("判题程序执行中遇到IO错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (DockerClientException e) {
            Result result = new Result();
            result.setLog(e.getClass().getName() + ": " + e.getMessage());
            result.setError("判题程序执行中遇到Docker错误。");
            result.setGradedAt(new Date());
            submission.setResult(result);
        } catch (Exception e) {
            Result result = new Result();
            result.setError(e.getMessage());
            result.setGradedAt(new Date());
            submission.setResult(result);
        } finally {
            repository.save(submission);
        }
    }

    @Override
    public int compareTo(GradeSubmissionTask o) {
        return this.priority - o.priority;
    }

}
