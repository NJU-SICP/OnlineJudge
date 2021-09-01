package cn.edu.nju.sicp.listeners;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.repositories.AssignmentRepository;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PruneType;
import com.rabbitmq.client.Channel;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class BuildImageListener implements ChannelAwareMessageListener {

    private final S3Config s3Config;
    private final DockerConfig dockerConfig;
    private final AssignmentRepository repository;
    private final Logger logger;

    public BuildImageListener(S3Config s3Config, DockerConfig dockerConfig,
            AssignmentRepository repository) {
        this.s3Config = s3Config;
        this.dockerConfig = dockerConfig;
        this.repository = repository;
        this.logger = LoggerFactory.getLogger(BuildImageListener.class);
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        logger.debug(String.format("Receive AMQP %s", message));
        String assignmentId = new String(message.getBody());
        try {
            Assignment assignment = repository.findById(assignmentId).orElseThrow();
            buildImage(assignment);
        } catch (NoSuchElementException ignored) {
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    private void buildImage(Assignment assignment) {
        S3Client s3 = s3Config.getInstance();
        DockerClient docker = dockerConfig.getInstance();
        StringBuilder logBuilder = new StringBuilder();

        logger.info(String.format("BuildImage start: %s", assignment));
        try {
            String key = assignment.getGrader().getKey();
            Path path = Files.createTempDirectory("sicp-build-workdir");
            Path temp = Paths.get(path.toString(), "temp.zip");

            if (dockerConfig.getRegistryUrl() != null) {
                String repo = assignment.getGrader().getImageRepository();
                repo = String.format("%s/%s", dockerConfig.getRegistryUrl(), repo);
                assignment.getGrader().setImageRepository(repo);
                repository.save(assignment);
            }

            s3.getObject(builder -> builder.bucket(s3Config.getBucket()).key(key).build(),
                    ResponseTransformer.toFile(temp.toFile()));
            try (ZipFile zipFile = new ZipFile(temp.toFile())) {
                zipFile.extractAll(path.toString());
            }
            Files.delete(temp);
            logBuilder.append(String.format("Extracted dockerfile archive to %s\n", path));

            try {
                docker.pruneCmd(PruneType.IMAGES).exec();

                String imageTag = String.format("%s:%s",
                        assignment.getGrader().getImageRepository(),
                        assignment.getGrader().getImageTag());
                String imageId = docker.buildImageCmd(path.toFile())
                        .withNoCache(true)
                        .withTags(Set.of(imageTag))
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
                        }).awaitImageId(5, TimeUnit.MINUTES);
                logBuilder.append(String.format("Built image %s\n", imageId));

                if (dockerConfig.getRegistryUrl() != null) {
                    docker.pushImageCmd(assignment.getGrader().getImageRepository())
                            .withTag(assignment.getGrader().getImageTag())
                            .start()
                            .awaitCompletion();
                    logBuilder.append(
                            String.format("Pushed image to %s\n", dockerConfig.getRegistryUrl()));
                }

                Date imageBuiltAt = new Date();
                assignment.getGrader().setImageId(imageId);
                assignment.getGrader().setImageBuiltAt(imageBuiltAt);
                logger.info(String.format("BuildImage succeed %s", assignment));
                logBuilder.append(String.format("Succeed at %s", imageBuiltAt));
            } catch (Exception e) {
                logger.error(String.format("%s %s", e.getMessage(), assignment), e);
                String error = String.format("\nFailed: %s %s\n%s", e.getClass().getName(),
                        e.getMessage(), ExceptionUtils.getStackTrace(e));
                logBuilder.append(error);
                assignment.getGrader().setImageBuildError(error);
            } finally {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            logger.error(String.format("%s %s", e.getMessage(), assignment), e);
            String error = String.format("\nFailed: %s %s\n%s", e.getClass().getName(),
                    e.getMessage(), ExceptionUtils.getStackTrace(e));
            logBuilder.append(error);
            assignment.getGrader().setImageBuildError(error);
        } finally {
            assignment.getGrader().setImageBuildLog(logBuilder.toString());
            repository.save(assignment);
        }
    }

}
