package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Plagiarism;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlagiarismRepository extends MongoRepository<Plagiarism, String> {
}
