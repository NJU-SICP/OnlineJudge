package cn.edu.nju.sicp.services;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import cn.edu.nju.sicp.configs.AmqpConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.contests.hog.HogConfig;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Example;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class SubmissionService {

    private final S3Config s3Config;
    private final SubmissionRepository repository;
    private final RabbitTemplate rabbit;
    private final Logger logger;

    public SubmissionService(S3Config s3Config, SubmissionRepository repository,
            RabbitTemplate rabbit) {
        this.s3Config = s3Config;
        this.repository = repository;
        this.rabbit = rabbit;
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

    public byte[] exportSubmissions(String assignmentId) throws IOException {
        Submission example = new Submission();
        example.setAssignmentId(assignmentId);
        Map<String, List<Submission>> submissionMap =
                repository.findAll(Example.of(example)).stream()
                        .collect(Collectors.groupingBy(Submission::getUserId));
        Pattern pattern = Pattern.compile("submissions/[^/]*/([^/]*)/(.*)");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            Collection<String> userIds = submissionMap.keySet();
            for (String userId : userIds) {
                // Store all submissions of a user.
                List<Submission> submissions = submissionMap.get(userId);
                for (Submission submission : submissions) {
                    try (InputStream s3InputStream = getSubmissionFile(submission)) {
                        Matcher matcher = pattern.matcher(submission.getKey());
                        String key = matcher.matches()
                                ? String.format("all/%s/%s", matcher.group(1), matcher.group(2))
                                : String.format("all/unknown/%s",
                                        submission.getKey().replace("/", "_"));
                        ZipEntry zipEntry = new ZipEntry(key);
                        zipOutputStream.putNextEntry(zipEntry);
                        IOUtils.copy(s3InputStream, zipOutputStream);
                        zipOutputStream.closeEntry();
                    } catch (S3Exception e) {
                        logger.error(String.format("Cannot export submission %s", submission), e);
                    }
                }
                // Store the first submission with highest score.
                Submission submission = submissions.stream().max((s1, s2) -> {
                    Integer v1 = s1.getResult() == null ? null : s1.getResult().getScore();
                    Integer v2 = s2.getResult() == null ? null : s2.getResult().getScore();
                    if (Objects.equals(v1, v2)) {
                        return -s1.getCreatedAt().compareTo(s2.getCreatedAt()); // older is larger
                    } else {
                        return v1 == null ? -1 : (v2 == null ? 1 : v1 - v2);
                    }
                }).orElse(null);
                if (submission != null) {
                    try (InputStream s3InputStream = getSubmissionFile(submission)) {
                        Matcher matcher = pattern.matcher(submission.getKey());
                        String key = matcher.matches()
                                ? String.format("single/%s_%s", matcher.group(1), matcher.group(2))
                                : String.format("single/unknown/%s",
                                        submission.getKey().replace("/", "_"));
                        ZipEntry zipEntry = new ZipEntry(key);
                        zipOutputStream.putNextEntry(zipEntry);
                        IOUtils.copy(s3InputStream, zipOutputStream);
                        zipOutputStream.closeEntry();
                    } catch (S3Exception e) {
                        logger.error(String.format("Cannot export submission %s", submission), e);
                    }
                }
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Scheduled(fixedRate = 10 * 1000) // per minute
    public void autoRetryGradingSubmissions() {
        try {
            List<Submission> submissions = repository.findAllByResultRetryAtBefore(new Date());
            if (submissions.size() > 0) {
                logger.info(String.format("Retry grading for %d submissions", submissions.size()));
            }
            for (Submission submission : submissions) {
                sendGradeSubmissionMessage(submission);
            }
        } catch (AmqpException e) {
            logger.error(String.format("Failed to retry grade submissions: %s", e.getMessage()), e);
        }
    }

    public void rejudgeSubmission(Submission submission) {
        submission.setGraded(false);
        submission.setResult(null);
        repository.save(submission);
        try {
            sendGradeSubmissionMessage(submission);
            logger.info(String.format("RejudgeSubmission %s", submission));
        } catch (Exception e) {
            logger.error(String.format("%s %s", e.getMessage(), submission), e);
        }
    }

    public void sendGradeSubmissionMessage(Submission submission) throws AmqpException {
        String exchange = AmqpConfig.directExchangeName;
        String routingKey = AmqpConfig.gradeSubmissionQueueName;
        String payload = submission.getId();
        rabbit.convertAndSend(exchange, routingKey, payload);
        logger.debug(String.format("Send AMQP exchange=%s routingKey=%s payload=%s",
                exchange, routingKey, payload));
    }

    // for Hog Contest
    public void sendHogMessage(Submission submission) throws AmqpException {
        String exchange = AmqpConfig.directExchangeName;
        String routingKey = HogConfig.hogQueueName;
        String payload = submission.getId();
        rabbit.convertAndSend(exchange, routingKey, payload);
        logger.debug(String.format("Send AMQP exchange=%s routingKey=%s payload=%s",
                exchange, routingKey, payload));
    }

}
