package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Extension;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExtensionRepository extends MongoRepository<Extension, String> {
}
