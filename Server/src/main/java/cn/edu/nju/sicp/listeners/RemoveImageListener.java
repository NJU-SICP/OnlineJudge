package cn.edu.nju.sicp.listeners;

import cn.edu.nju.sicp.configs.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RemoveImageListener implements MessageListener {

    private final DockerConfig dockerConfig;
    private final Logger logger;

    public RemoveImageListener(DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
        this.logger = LoggerFactory.getLogger(RemoveImageListener.class);
    }

    @Override
    public void onMessage(Message message) {
        logger.debug(String.format("Receive AMQP %s", message));
        removeImage(new String(message.getBody()));
    }

    private void removeImage(String imageId) {
        DockerClient docker = dockerConfig.getInstance();
        logger.info(String.format("RemoveImage imageId=%s", imageId));
        try {
            List<Container> containers = docker.listContainersCmd().withShowAll(true).exec();
            containers.forEach((container) -> {
                if (Objects.equals(container.getImage(), imageId)) {
                    logger.info(String.format("RemoveImage will remove container %s",
                            container.getId()));
                    try {
                        docker.stopContainerCmd(container.getId()).withTimeout(0).exec();
                    } catch (NotModifiedException ignored) {
                    }
                    docker.removeContainerCmd(container.getId())
                            .withForce(true)
                            .withRemoveVolumes(true)
                            .exec();
                }
            });
            docker.removeImageCmd(imageId).withForce(true).exec();
        } catch (DockerClientException e) {
            logger.warn(
                    String.format("RemoveImage failed: %s imageId=%s", e.getMessage(), imageId));
        }
    }

}
