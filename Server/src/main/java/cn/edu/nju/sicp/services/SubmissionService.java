package cn.edu.nju.sicp.services;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import cn.edu.nju.sicp.configs.AmqpConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Submission;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class SubmissionService {

    private final S3Config s3Config;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger;

    public SubmissionService(S3Config s3Config, RabbitTemplate rabbitTemplate) {
        this.s3Config = s3Config;
        this.rabbitTemplate = rabbitTemplate;
        this.logger = LoggerFactory.getLogger(SubmissionService.class);
    }

    public void uploadSubmissionFile(Submission submission, InputStream stream, long size)
            throws S3Exception {
        S3Client s3 = s3Config.getInstance();
        String bucket = s3Config.getBucket();
        String key = submission.getKey();
        RequestBody requestBody = RequestBody.fromInputStream(stream, size);
        s3.putObject(builder -> builder.bucket(bucket).key(key).build(), requestBody);
        logger.debug(String.format("Put submission file %s", key));
    }

    public InputStream getSubmissionFile(Submission submission) throws S3Exception {
        S3Client s3 = s3Config.getInstance();
        String bucket = s3Config.getBucket();
        String key = submission.getKey();
        return s3.getObject(builder -> builder.bucket(bucket).key(key).build());
    }

    public void deleteSubmissionFile(Submission submission) throws S3Exception {
        S3Client s3 = s3Config.getInstance();
        String bucket = s3Config.getBucket();
        String key = submission.getKey();
        s3.deleteObject(builder -> builder.bucket(bucket).key(key).build());
        logger.debug(String.format("Delete submission file %s", key));
    }

    public void sendGradeSubmissionMessage(Submission submission) throws AmqpException {
        String exchange = AmqpConfig.directExchangeName;
        String routingKey = AmqpConfig.gradeSubmissionQueueName;
        String payload = submission.getId();
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        logger.debug(String.format("Send AMQP exchange=%s routingKey=%s payload=%s",
                exchange, routingKey, payload));
    }

}
