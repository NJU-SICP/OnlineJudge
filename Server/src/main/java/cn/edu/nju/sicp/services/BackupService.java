package cn.edu.nju.sicp.services;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.models.Backup;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class BackupService {

  private final S3Config s3Config;
  private final Logger logger;

  public BackupService(S3Config s3Config) {
    this.s3Config = s3Config;
    this.logger = LoggerFactory.getLogger(BackupService.class);
  }

  public void uploadBackupFile(Backup backup, InputStream stream, long size) throws S3Exception {
    S3Client s3 = s3Config.getInstance();
    String bucket = s3Config.getBucket();
    String key = backup.getKey();
    RequestBody requestBody = RequestBody.fromInputStream(stream, size);
    s3.putObject(builder -> builder.bucket(bucket).key(key).build(), requestBody);
    logger.debug(String.format("Put backup file %s", key));
  }

  public InputStream getBackupFile(Backup backup) throws S3Exception {
    S3Client s3 = s3Config.getInstance();
    String bucket = s3Config.getBucket();
    String key = backup.getKey();
    return s3.getObject(builder -> builder.bucket(bucket).key(key).build());
  }

  public void deleteBackupFile(Backup backup) throws S3Exception {
    S3Client s3 = s3Config.getInstance();
    String bucket = s3Config.getBucket();
    String key = backup.getKey();
    s3.deleteObject(builder -> builder.bucket(bucket).key(key).build());
    logger.debug(String.format("Delete backup file %s", key));
  }

}
