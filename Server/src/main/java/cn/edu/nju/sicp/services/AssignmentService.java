package cn.edu.nju.sicp.services;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import cn.edu.nju.sicp.configs.AmqpConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Grader;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class AssignmentService {

    private final S3Config s3Config;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger;

    public AssignmentService(S3Config s3Config, RabbitTemplate rabbitTemplate) {
        this.s3Config = s3Config;
        this.rabbitTemplate = rabbitTemplate;
        this.logger = LoggerFactory.getLogger(AssignmentService.class);
    }

    public void uploadGraderFile(Assignment assignment, Grader grader, InputStream stream,
            long size) throws S3Exception {
        S3Client s3 = s3Config.getInstance();
        String bucket = s3Config.getBucket();
        String key = grader.getKey();
        RequestBody requestBody = RequestBody.fromInputStream(stream, size);
        s3.putObject(builder -> builder.bucket(bucket).key(key).build(), requestBody);
        logger.debug(String.format("Put grader file %s", key));

        assignment.setGrader(grader);
    }

    public void deleteGraderFile(Assignment assignment) throws S3Exception, AmqpException {
        Grader grader = assignment.getGrader();
        if (grader != null) {
            S3Client s3 = s3Config.getInstance();
            String bucket = s3Config.getBucket();
            String key = grader.getKey();
            s3.deleteObject(builder -> builder.bucket(bucket).key(key).build());
            logger.debug(String.format("Delete grader file %s", key));
        }
    }

    public void sendBuildImageMessage(Assignment assignment) throws AmqpException {
        String exchange = AmqpConfig.directExchangeName;
        String routingKey = AmqpConfig.buildImageQueueName;
        String payload = assignment.getId();
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        logger.debug(String.format("Send AMQP exchange=%s routingKey=%s payload=%s",
                exchange, routingKey, payload));
    }

    public void sendRemoveImageMessage(Assignment assignment) throws AmqpException {
        Grader grader = assignment.getGrader();
        if (grader != null && grader.getImageId() != null) {
            String exchange = AmqpConfig.fanoutExchangeName;
            String routingKey = AmqpConfig.removeImageQueueName;
            String payload = grader.getImageId();
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
            logger.debug(String.format("Send AMQP exchange=%s routingKey=%s payload=%s",
                    exchange, routingKey, payload));
        }
    }

}
