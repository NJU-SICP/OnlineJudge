package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Result;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public GradeSubmissionTask(Assignment assignment, Submission submission, SubmissionRepository repository) {
        this(assignment, submission, repository, PRIORITY_NORM);
    }

    public GradeSubmissionTask(Assignment assignment, Submission submission, SubmissionRepository repository, int priority) {
        this.assignment = assignment;
        this.submission = submission;
        this.repository = repository;
        this.priority = priority;
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
            result.setRetry(retry.getTime());
            submission.setResult(result);
            repository.save(submission);
            return;
        }

        try {
            DockerClient client = DockerConfig.getInstance();
            String containerId = client.createContainerCmd(imageId).exec().getId();

            File file = Paths.get(submission.getFilePath()).toFile();
            TarArchiveInputStream stream = new TarArchiveInputStream(new FileInputStream(file));
            client.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(stream)
                    .withRemotePath(String.format("/workdir/submit%s", assignment.getSubmitFileType()))
                    .exec();
            client.startContainerCmd(containerId).exec();
            client.waitContainerCmd(containerId).exec(new WaitContainerResultCallback()).awaitStatusCode();
            InputStream result = client.copyArchiveFromContainerCmd(containerId, "/workdir/result.json").exec();
            // FIXME: read result from json in tar archive
        } catch (IOException e) {

        } catch (DockerClientException e) {

        } finally {

        }
    }

    @Override
    public int compareTo(GradeSubmissionTask o) {
        return this.priority - o.priority;
    }

}
