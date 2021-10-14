package cn.edu.nju.sicp.contests.hog;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.HostConfig;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;
import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

@Component
public class HogListener implements MessageListener {

    private final S3Config s3Config;
    private final DockerConfig dockerConfig;
    private final SubmissionRepository submissionRepository;
    private final HogRepository hogRepository;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public HogListener(S3Config s3Config, DockerConfig dockerConfig,
            SubmissionRepository submissionRepository, HogRepository hogRepository) {
        this.s3Config = s3Config;
        this.dockerConfig = dockerConfig;
        this.submissionRepository = submissionRepository;
        this.hogRepository = hogRepository;
    }

    @Override
    public void onMessage(Message message) {
        logger.debug(String.format("Receive AMQP %s", message));
        String submissionId = new String(message.getBody());
        Optional<Submission> optionalSubmission = submissionRepository.findById(submissionId);
        if (optionalSubmission.isPresent()) {
            updateHogContest(optionalSubmission.get());
        }
    }

    /**
     * The update process is lock-free. First, create a new entry and set all previous entries
     * invalid. Then get a list of all entries that is valid and created before. Finally, play this
     * strategy against all previous entries and update result using atomic set.
     **/
    private void updateHogContest(Submission submission) {
        logger.info(String.format("UpdateHogContest submission=%s", submission));
        S3Client s3 = s3Config.getInstance();
        DockerClient docker = dockerConfig.getInstance();
        HogEntry entry = generateHogEntry(s3, docker, submission);
        if (entry == null) {
//            List<HogEntry> entries = hogRepository.findAll
        }
    }

    private HogEntry generateHogEntry(S3Client s3, DockerClient docker, Submission submission) {
        try {
            Path temp = Files.createTempFile("generate-tar", ".tar");
            try (FileOutputStream tempOutputStream = new FileOutputStream(temp.toFile());
                    ArchiveOutputStream tarOutputStream =
                            new TarArchiveOutputStream(tempOutputStream);
                    InputStream s3Stream = s3.getObject(builder -> builder
                            .bucket(s3Config.getBucket())
                            .key(submission.getKey())
                            .build())) {
                byte[] s3Bytes = IOUtils.toByteArray(s3Stream);
                TarArchiveEntry entry = new TarArchiveEntry("final_strategy.py");
                entry.setSize(s3Bytes.length);
                tarOutputStream.putArchiveEntry(entry);
                try (ByteArrayInputStream byteArrayInputStream =
                        new ByteArrayInputStream(s3Bytes)) {
                    IOUtils.copy(byteArrayInputStream, tarOutputStream);
                }
                tarOutputStream.closeArchiveEntry();
            }

            String containerId = docker.createContainerCmd(HogConfig.hogImage1)
                    .withNetworkDisabled(true)
                    .withHostConfig(new HostConfig()
                            .withCpuPeriod(100000L)
                            .withCpuQuota(100000L)
                            .withMemory(1024L * 1024 * 1024)
                            .withMemorySwappiness(0L))
                    .exec()
                    .getId();
            docker.copyArchiveToContainerCmd(containerId)
                    .withTarInputStream(Files.newInputStream(temp)).withRemotePath("/workdir")
                    .exec();
            docker.startContainerCmd(containerId).exec();
            if (!docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback())
                    .awaitCompletion(300, TimeUnit.SECONDS)) {
                throw new Exception("判题容器执行程序超时。");
            }
            try (InputStream resultStream = docker
                    .copyArchiveFromContainerCmd(containerId, "/workdir/strategy.json")
                    .exec();
                    TarArchiveInputStream tarInputStream =
                            new TarArchiveInputStream(resultStream)) {
                docker.removeContainerCmd(containerId).exec();
                ArchiveEntry entry = tarInputStream.getNextEntry();
                if (entry == null || !tarInputStream.canReadEntryData(entry)) {
                    throw new Exception("没有保存结果或保存的结果已损坏。");
                } else {
                    HogEntry hogEntry = new HogEntry();
                    hogEntry.setName(name); // WIP, consider move to gradeSubmissionListener
                    hogEntry.setUserId(submission.getUserId());
                    hogEntry.setKey(String.format("contests/hog/submission-%s-strategy.json", submission.getId()));
                    hogEntry.setDate(new Date());
                    hogEntry.setValid(true);
                    hogEntry.setRates(new HashMap<String, Double>());

                    RequestBody requestBody = RequestBody
                            .fromInputStream(tarInputStream, entry.getSize());
                    s3.putObject(builder -> builder
                            .bucket(s3Config.getBucket())
                            .key(hogEntry.getKey())
                            .build(), requestBody);
                    
                    hogRepository.save(hogEntry);
                    return hogEntry;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

}
