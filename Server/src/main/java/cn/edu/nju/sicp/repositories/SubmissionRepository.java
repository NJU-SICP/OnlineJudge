package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

}
