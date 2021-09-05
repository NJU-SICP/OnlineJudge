package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

    List<Submission> findAllByResultRetryAtBefore(Date date);

}
