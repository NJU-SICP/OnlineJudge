package cn.edu.nju.sicp.dtos;

import cn.edu.nju.sicp.models.Result;
import cn.edu.nju.sicp.models.Submission;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "SubmissionInfo", types = Submission.class)
public interface SubmissionInfo {

    String getId();

    String getUserId();

    String getAssignmentId();

    String getKey();

    Date getCreatedAt();

    String getCreatedBy();

    Result getResult();

}
