package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.docker.Client;
import cn.edu.nju.sicp.docker.Grader;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.BuildResponseItem;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

public class BuildImageTask implements Runnable {

    private final Logger logger;
    private final Grader grader;
    private final AssignmentRepository repository;

    public BuildImageTask(Grader grader, AssignmentRepository repository) {
        this.logger = LoggerFactory.getLogger(BuildImageTask.class);
        this.grader = grader;
        this.repository = repository;
    }

    public void run() {
        Optional<Assignment> optionalAssignment = repository.findById(grader.getAssignmentId());
        if (optionalAssignment.isEmpty()) {
            logger.error(String.format("BuildImage error: assignment %s is not present", grader.getAssignmentId()));
            return;
        }
        Assignment assignment = optionalAssignment.get();
        Grader grader = assignment.getGrader();
        StringBuilder logBuilder = new StringBuilder();
        logger.info(String.format("BuildImage start: %s", assignment));

        try {
            Path path = Files.createTempDirectory("sicp-build-workdir");
            (new ZipFile(Paths.get(grader.getDockerfilePath()).toFile())).extractAll(path.toString());
            logBuilder.append(String.format("Extracted dockerfile archive to %s", path.toString()));
            try {
                DockerClient client = Client.getInstance();
                StringBuilder buildLogBuilder = new StringBuilder();
                String imageId = client.buildImageCmd(path.toFile())
                        .withNoCache(true)
                        .withTags(grader.getImageTags())
                        .exec(new BuildImageResultCallback() {
                            @Override
                            public void onNext(BuildResponseItem item) {
                                String stream = item.getStream();
                                if (stream != null) {
                                    buildLogBuilder.append(stream);
                                    grader.setImageBuildLog(buildLogBuilder.toString());
                                    repository.save(assignment);
                                }
                                super.onNext(item);
                            }
                        })
                        .awaitImageId();
                logBuilder.append(buildLogBuilder.toString());
                Date imageBuiltAt = new Date();

                grader.setImageId(imageId);
                grader.setImageBuiltAt(imageBuiltAt);
                logger.info(String.format("BuildImage succeed at %s %s", imageBuiltAt, assignment));
                logBuilder.append(String.format("BuildImage succeed at %s %s", imageBuiltAt, assignment));
            } catch (DockerClientException e) {
                logger.error(String.format("BuildImage failed: %s %s", e.getMessage(), assignment));
                logBuilder.append(String.format("BuildImage failed: %s %s", e.getMessage(), assignment));
            } finally {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            logger.error(String.format("BuildImage failed: %s %s", e.getMessage(), assignment));
            logBuilder.append(String.format("BuildImage failed: %s %s", e.getMessage(), assignment));
        } finally {
            grader.setImageBuildLog(logBuilder.toString());
            repository.save(assignment);
        }
    }

}
