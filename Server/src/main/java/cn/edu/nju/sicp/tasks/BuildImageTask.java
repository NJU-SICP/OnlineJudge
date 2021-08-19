package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import net.lingala.zip4j.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private final DockerClient client;

    public BuildImageTask(Grader grader, AssignmentRepository repository, DockerClient client) {
        this.logger = LoggerFactory.getLogger(BuildImageTask.class);
        this.grader = grader;
        this.repository = repository;
        this.client = client;
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
            (new ZipFile(Paths.get(grader.getFilePath()).toFile())).extractAll(path.toString());
            logBuilder.append(String.format("Extracted dockerfile archive to %s\n", path.toString()));
            try {
                String imageId = client.buildImageCmd(path.toFile())
                        .withNoCache(true)
                        .withTags(grader.getImageTags())
                        .exec(new BuildImageResultCallback() {
                            @Override
                            public void onNext(BuildResponseItem item) {
                                String stream = item.getStream();
                                if (stream != null) {
                                    logBuilder.append(stream);
                                    grader.setImageBuildLog(logBuilder.toString());
                                    repository.save(assignment);
                                }
                                super.onNext(item);
                            }
                        })
                        .awaitImageId();
                Date imageBuiltAt = new Date();
                grader.setImageId(imageId);
                grader.setImageBuiltAt(imageBuiltAt);
                logger.info(String.format("BuildImage succeed at %s %s", imageBuiltAt, assignment));
                logBuilder.append(String.format("BuildImage succeed at %s %s", imageBuiltAt, assignment));
            } catch (Exception e) {
                String error = String.format("BuildImage failed: %s %s %s", e.getClass().getName(), e.getMessage(), assignment);
                logger.error(error);
                logBuilder.append(error);
                grader.setImageBuildError(error);
            } finally {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            String error = String.format("BuildImage failed: %s %s %s", e.getClass().getName(), e.getMessage(), assignment);
            logger.error(error);
            logBuilder.append(error);
            grader.setImageBuildError(error);
        } finally {
            grader.setImageBuildLog(logBuilder.toString());
            repository.save(assignment);
        }
    }

}
