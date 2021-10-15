package cn.edu.nju.sicp.contests.hog;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.mongodb.client.MongoClients;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
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
    private final MongoOperations mongoOps = // we want to use atomic update operations here
            new MongoTemplate(new SimpleMongoClientDatabaseFactory(MongoClients.create(), HogConfig.collection));
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogCompare(S3Config s3Config, DockerConfig dockerConfig) {
        this.s3 = s3Config.getInstance();
        this.s3Bucket = s3Config.getBucket();
        this.docker = dockerConfig.getInstance();
    }

    @Override
    public void accept(HogEntry entry) {
        logger.info(String.format("UpdateHogContest start entry=%s", entry));
        List<HogEntry> opponents = mongoOps.find(Query.query(Criteria.where("valid").is(true)
                .andOperator(Criteria.where("date").lt(entry.getDate()))), HogEntry.class);
        try {
            for (HogEntry opponent : opponents) {
                HogCompareResult result = compareHogEntries(entry, opponent);
                if (result.getWins() + result.getLoses() != HogConfig.compareRounds) {
                    throw new Exception("胜负局数之和不等于抽样数量。");
                }
                mongoOps.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                        Update.update(String.format("wins.%s", opponent.getId()), result.getWins()), HogEntry.class);
                mongoOps.updateFirst(Query.query(Criteria.where("_id").is(opponent.getId())),
                        Update.update(String.format("wins.%s", entry.getId()), result.getLoses()), HogEntry.class);
            }
        } catch (Exception e) {
            logger.error(String.format("UpdateHogContest failed: %s", e.getMessage()), e);
            mongoOps.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                    Update.update("valid", false), HogEntry.class);
            mongoOps.updateFirst(Query.query(Criteria.where("_id").is(entry.getId())),
                    Update.update("message", e.getMessage()), HogEntry.class);
        }
        logger.info(String.format("UpdateHogContest complete entry=%s", entry));
    }

    private HogCompareResult compareHogEntries(HogEntry entry1, HogEntry entry2) throws Exception {
        InspectImageResponse inspect = docker.inspectImageCmd(HogConfig.compareImage).exec();
        ContainerConfig config = inspect.getConfig();
        List<String> args = new ArrayList<>(List.of("/usr/bin/python3", "compare.py"));
        if (config != null && config.getEntrypoint() != null) {
            args = new ArrayList<>(List.of(config.getEntrypoint()));
        }
        args.add(String.format("%d", HogConfig.compareRounds));

        String containerId = docker.createContainerCmd(HogConfig.compareImage)
                .withEntrypoint(args.toArray(new String[0]))
                .withNetworkDisabled(true)
                .withHostConfig(new HostConfig()
                        .withCpuPeriod(100000L)
                        .withCpuQuota(100000L)
                        .withMemory(256L * 1024 * 1024)
                        .withMemorySwappiness(0L))
                .exec()
                .getId();

        Path temp = Files.createTempFile("compare-data", ".tar");
        Path json = Files.createTempFile("compare-result", ".json");

        try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
             ArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(tempOutputStream);
             InputStream s3Stream1 = s3.getObject(builder -> builder.bucket(s3Bucket).key(entry1.getKey()).build());
             InputStream s3Stream2 = s3.getObject(builder -> builder.bucket(s3Bucket).key(entry2.getKey()).build())) {
            int count = 0;
            for (var s3Stream : List.of(s3Stream1, s3Stream2)) {
                ++count;
                byte[] s3Bytes = IOUtils.toByteArray(s3Stream);
                String archiveFilename = String.format("strategy%d.json", count);
                TarArchiveEntry archiveEntry = new TarArchiveEntry(archiveFilename);
                archiveEntry.setSize(s3Bytes.length);
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
                .awaitCompletion(180, TimeUnit.SECONDS)) {
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
