package cn.edu.nju.sicp.contests.hog;

import cn.edu.nju.sicp.configs.AmqpConfig;
import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Submission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class HogTrigger implements Consumer<Submission> {

    private final S3Client s3;
    private final String s3Bucket;
    private final DockerClient docker;
    private final HogRepository hogRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogTrigger(S3Config s3Config,
                      DockerConfig dockerConfig,
                      HogRepository hogRepository,
                      RabbitTemplate rabbitTemplate) {
        this.s3 = s3Config.getInstance();
        this.s3Bucket = s3Config.getBucket();
        this.docker = dockerConfig.getInstance();
        this.hogRepository = hogRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void accept(Submission submission) {
        generateHogStrategy(submission);
    }

    private void generateHogStrategy(Submission submission) {
        logger.error(String.format("GenerateHogStrategy submission=%s", submission));
        HogEntry entry = new HogEntry();
        entry.setUserId(submission.getUserId());
        entry.setName(null);
        entry.setKey(null);
        entry.setWins(new HashMap<>());
        try {
            InputStream stream = s3.getObject(builder ->
                    builder.bucket(s3Bucket).key(submission.getKey()).build());
            HogTriggerResult result = runGenerateImage(stream);
            entry.setName(result.getName());
            entry.setValid(true);
            hogRepository.save(entry);

            entry.setKey(String.format("contests/hog/%s.json", entry.getId()));
            RequestBody requestBody = RequestBody.fromString((new ObjectMapper()).writeValueAsString(result));
            s3.putObject(builder -> builder.bucket(s3Bucket).key(entry.getKey()).build(), requestBody);
            hogRepository.save(entry);
        } catch (Exception e) {
            logger.error(String.format("GenerateHogStrategy failed submission=%s", submission), e);
            entry.setValid(false);
            entry.setMessage(e.getMessage());
        } finally {
            entry.setDate(new Date());
            hogRepository.save(entry);
            if (entry.isValid()) {
                rabbitTemplate.convertAndSend(AmqpConfig.directExchangeName, HogConfig.queueName, entry.getId());
            }
        }
    }

    private HogTriggerResult runGenerateImage(InputStream submissionInputStream) throws Exception {
        Path temp = Files.createTempFile("hog-contest-file", ".tar");
        Path json = Files.createTempFile("hog-contest-json", ".json");

        String containerId = docker.createContainerCmd(HogConfig.triggerImage)
                .withNetworkDisabled(true)
                .withHostConfig(new HostConfig()
                        .withCpuPeriod(100000L)
                        .withCpuQuota(100000L)
                        .withMemory(1024L * 1024 * 1024) // 1G RAM
                        .withMemorySwappiness(0L))
                .exec()
                .getId();

        try (FileOutputStream fileOutputStream = new FileOutputStream(temp.toFile());
             TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(fileOutputStream)) {
            byte[] bytes = IOUtils.toByteArray(submissionInputStream);
            TarArchiveEntry entry = new TarArchiveEntry("strategy.py");
            entry.setSize(bytes.length);
            tarOutputStream.putArchiveEntry(entry);
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                IOUtils.copy(byteArrayInputStream, tarOutputStream);
            }
            tarOutputStream.closeArchiveEntry();
        }

        docker.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(Files.newInputStream(temp))
                .withRemotePath("/workdir")
                .exec();
        docker.startContainerCmd(containerId).exec();
        if (!docker.waitContainerCmd(containerId)
                .exec(new WaitContainerResultCallback())
                .awaitCompletion(600, TimeUnit.SECONDS)) {
            throw new Exception("Hog策略生成程序超时。");
        }

        try (InputStream resultStream =
                     docker.copyArchiveFromContainerCmd(containerId, "/workdir/result.json").exec();
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(resultStream)) {
            docker.removeContainerCmd(containerId).exec();
            ArchiveEntry entry = tarInputStream.getNextEntry();
            if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                throw new Exception("无法读取Hog策略生成程序保存的结果。");
            } else {
                try (OutputStream jsonStream = Files.newOutputStream(json)) {
                    IOUtils.copy(tarInputStream, jsonStream);
                }
            }
        }
        HogTriggerResult result = (new ObjectMapper()).readValue(Files.newInputStream(json), HogTriggerResult.class);

        Files.deleteIfExists(temp);
        Files.deleteIfExists(json);
        return result;
    }

}
