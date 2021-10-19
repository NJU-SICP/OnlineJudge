package cn.edu.nju.sicp.contests.hog;

import cn.edu.nju.sicp.configs.AmqpConfig;
import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class HogCompare implements Consumer<HogEntry> {

    private final S3Client s3;
    private final String s3Bucket;
    private final DockerClient docker;
    private final RabbitTemplate rabbit;
    private final MongoOperations mongo;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogCompare(S3Config s3Config,
                      DockerConfig dockerConfig,
                      RabbitTemplate rabbit,
                      MongoTemplate mongo) {
        this.s3 = s3Config.getInstance();
        this.s3Bucket = s3Config.getBucket();
        this.docker = dockerConfig.getInstance();
        this.rabbit = rabbit;
        this.mongo = mongo;
    }

    @Override
    public void accept(HogEntry entry) {
        logger.info(String.format("UpdateHogContest start entry=%s", entry));
        List<HogEntry> opponents = mongo.find(Query.query(Criteria.where("valid").is(true)
                .andOperator(Criteria.where("date").lt(entry.getDate()))), HogEntry.class);
        if (opponents.stream().anyMatch(o -> o.getKey() == null)) {
            logger.info(String.format("UpdateHogContest will try later entry=%s", entry));
            try {
                Thread.sleep(60 * 1000); // retry at one minute later
            } catch (InterruptedException ignored) {
                rabbit.convertAndSend(AmqpConfig.directExchangeName, HogConfig.queueName, entry.getId());
            }
        }

        try {
            for (HogEntry opponent : opponents) {
                if (mongo.count(Query.query(Criteria.where("id").is(entry.getId())
                        .andOperator(Criteria.where("valid").is(true))), HogEntry.class) == 0) {
                    break; // the entry is invalid now, skip all
                }
                if (mongo.count(Query.query(Criteria.where("id").is(opponent.getId())
                        .andOperator(Criteria.where("valid").is(true))), HogEntry.class) == 0) {
                    continue; // the opponent is invalid now, skip it
                }

                logger.info(String.format("UpdateHogContest play %s vs %s", entry, opponent));
                HogCompareResult result = compareHogEntries(entry, opponent);
                if (result.getWins() + result.getLoses() != HogConfig.compareRounds) {
                    throw new Exception("胜负局数之和不等于抽样数量。");
                }
                mongo.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                        Update.update(String.format("wins.%s", opponent.getId()), result.getWins()), HogEntry.class);
                mongo.updateFirst(Query.query(Criteria.where("_id").is(opponent.getId())),
                        Update.update(String.format("wins.%s", entry.getId()), result.getLoses()), HogEntry.class);
            }
            logger.info(String.format("UpdateHogContest OK entry=%s", entry));
        } catch (Exception e) {
            mongo.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                    Update.update("valid", false), HogEntry.class);
            mongo.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                    Update.update("message", e.getMessage()), HogEntry.class);
            logger.error(String.format("UpdateHogContest failed: %s entry=%s", e.getMessage(), entry), e);
        }
    }

    private HogCompareResult compareHogEntries(HogEntry entry0, HogEntry entry1) throws Exception {
        Path temp = Files.createTempFile("compare-data", ".tar");
        Path json = Files.createTempFile("compare-result", ".json");

        String containerId = docker.createContainerCmd(HogConfig.compareImage)
                .withNetworkDisabled(true)
                .withHostConfig(new HostConfig()
                        .withCpuPeriod(100000L)
                        .withCpuQuota(100000L)
                        .withMemory(1024L * 1024 * 1024) // load scenarios require huge memory
                        .withMemorySwappiness(0L))
                .exec()
                .getId();

        try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
             ArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tempOutputStream);
             InputStream s3Stream0 = s3.getObject(builder -> builder.bucket(s3Bucket).key(entry0.getKey()).build());
             InputStream s3Stream1 = s3.getObject(builder -> builder.bucket(s3Bucket).key(entry1.getKey()).build())) {
            int count = 0; // 0 and 1
            for (var s3Stream : List.of(s3Stream0, s3Stream1)) {
                byte[] s3Bytes = IOUtils.toByteArray(s3Stream);
                String archiveFilename = String.format("strategy%d.json", count++);
                TarArchiveEntry archiveEntry = new TarArchiveEntry(archiveFilename);
                archiveEntry.setSize(s3Bytes.length);
                tarOutputStream.putArchiveEntry(archiveEntry);
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s3Bytes)) {
                    IOUtils.copy(byteArrayInputStream, tarOutputStream);
                }
                tarOutputStream.closeArchiveEntry();
            }
        }
        docker.copyArchiveToContainerCmd(containerId)
                .withTarInputStream(Files.newInputStream(temp)).withRemotePath("/workdir")
                .exec();
        docker.startContainerCmd(containerId).exec();
        if (!docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())
                .awaitCompletion(1800, TimeUnit.SECONDS)) {
            throw new Exception("比较容器执行程序超时。");
        }

        try (InputStream resultStream = docker.copyArchiveFromContainerCmd(containerId, "/workdir/result.json").exec();
             TarArchiveInputStream tarInputStream = new TarArchiveInputStream(resultStream)) {
            docker.removeContainerCmd(containerId).exec();
            ArchiveEntry entry = tarInputStream.getNextEntry();
            if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                throw new Exception("进行比较后没有保存结果或保存的结果已损坏。");
            } else {
                try (OutputStream jsonStream = Files.newOutputStream(json)) {
                    IOUtils.copy(tarInputStream, jsonStream);
                }
            }
        }
        return (new ObjectMapper()).readValue(json.toFile(), HogCompareResult.class);
    }

}
