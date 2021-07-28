package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.dtos.SubmissionInfo;
import cn.edu.nju.sicp.models.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(excerptProjection = SubmissionInfo.class)
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Page<Submission> findByAssignmentIdOrderByCreatedAtDesc(String assignmentId, Pageable pageable);

    List<Submission> findByUserIdAndAssignmentIdOrderByCreatedAtDesc(String userId, String assignmentId);

    int countByUserIdAndAssignmentId(String userId, String assignmentId);

}
