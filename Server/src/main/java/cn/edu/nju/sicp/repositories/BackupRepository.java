package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Backup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BackupRepository extends MongoRepository<Backup, String> {
}
