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
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

public class BuildImageTask implements Runnable {

    private final Assignment assignment;
    private final AssignmentRepository repository;
    private final String s3Bucket;
    private final S3Client s3Client;
    private final DockerClient client;
    private final Logger logger;

    public BuildImageTask(Assignment assignment,
                          AssignmentRepository repository,
                          String s3Bucket,
                          S3Client s3Client,
                          DockerClient client) {
        this.assignment = assignment;
        this.repository = repository;
        this.s3Bucket = s3Bucket;
        this.s3Client = s3Client;
        this.client = client;
        this.logger = LoggerFactory.getLogger(BuildImageTask.class);
    }

    public void run() {
        StringBuilder logBuilder = new StringBuilder();
        logger.info(String.format("BuildImage start: %s", assignment));

        try {
            String key = assignment.getGrader().getKey();
            Path path = Files.createTempDirectory("sicp-build-workdir");
            Path temp = Paths.get(path.toString(), "temp.zip");

            s3Client.getObject(builder -> builder.bucket(s3Bucket).key(key).build(),
                    ResponseTransformer.toFile(temp.toFile()));
            (new ZipFile(temp.toFile())).extractAll(path.toString());
            Files.delete(temp);
            logBuilder.append(String.format("Extracted dockerfile archive to %s\n", path));

            try {
                String imageId = client.buildImageCmd(path.toFile())
                        .withNoCache(true)
                        .withTags(assignment.getGrader().getImageTags())
                        .exec(new BuildImageResultCallback() {
                            @Override
                            public void onNext(BuildResponseItem item) {
                                String stream = item.getStream();
                                if (stream != null) {
                                    logBuilder.append(stream);
                                    assignment.getGrader().setImageBuildLog(logBuilder.toString());
                                    repository.save(assignment);
                                }
                                super.onNext(item);
                            }
                        })
                        .awaitImageId();
                Date imageBuiltAt = new Date();
                assignment.getGrader().setImageId(imageId);
                assignment.getGrader().setImageBuiltAt(imageBuiltAt);
                logBuilder.append(String.format("BuildImage succeed at %s %s", imageBuiltAt, assignment));
            } catch (Exception e) {
                String error = String.format("BuildImage failed: %s %s", e.getClass().getName(), e.getMessage());
                logBuilder.append(error);
                assignment.getGrader().setImageBuildError(error);
            } finally {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            String error = String.format("BuildImage failed: %s %s", e.getClass().getName(), e.getMessage());
            logBuilder.append(error);
            assignment.getGrader().setImageBuildError(error);
        } finally {
            assignment.getGrader().setImageBuildLog(logBuilder.toString());
            repository.save(assignment);
            if (assignment.getGrader().getImageBuildError() == null) {
                logger.info(String.format("BuildImage succeed at %s %s",
                        assignment.getGrader().getImageBuiltAt(), assignment));
            } else {
                logger.error(String.format("%s %s", assignment.getGrader().getImageBuildError(), assignment));
            }
        }
    }

}
