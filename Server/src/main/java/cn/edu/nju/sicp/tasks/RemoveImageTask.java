package cn.edu.nju.sicp.tasks;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.models.Grader;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RemoveImageTask implements Runnable {

    private final String imageId;
    private final Set<String> imageTags;
    private final DockerClient client;
    private final Logger logger;

    public RemoveImageTask(Grader grader, DockerClient client) {
        this.imageId = grader.getImageId();
        this.imageTags = grader.getImageTags();
        this.client = client;
        this.logger = LoggerFactory.getLogger(RemoveImageTask.class);
    }

    @Override
    public void run() {
        if (imageId == null) return;
        logger.info(String.format("RemoveImage imageId=%s imageTags=%s", imageId, imageTags));
        try {
            List<Container> containers = client.listContainersCmd().withShowAll(true).exec();
            containers.forEach((container) -> {
                if (Objects.equals(container.getImage(), imageId)) {
                    logger.info(String.format("RemoveImage will remove container %s", container.getId()));
                    try {
                        client.stopContainerCmd(container.getId()).withTimeout(0).exec();
                    } catch (NotModifiedException ignored) {
                    }
                    client.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
                }
            });
            client.removeImageCmd(imageId).withForce(true).exec();
        } catch (DockerClientException e) {
            logger.warn(String.format("RemoveImage failed: %s imageId=%s imageTags=%s", e.getMessage(), imageId, imageTags));
        }
    }
}
