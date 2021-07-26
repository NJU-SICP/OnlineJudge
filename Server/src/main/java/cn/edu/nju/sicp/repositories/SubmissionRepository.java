package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.dtos.SubmissionInfo;
import cn.edu.nju.sicp.models.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(excerptProjection = SubmissionInfo.class)
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    int countByUserIdAndAssignmentId(String userId, String assignmentId);

}
