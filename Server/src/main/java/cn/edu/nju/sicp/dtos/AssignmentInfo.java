package cn.edu.nju.sicp.dtos;

import cn.edu.nju.sicp.models.Assignment;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "AssignmentInfo", types = Assignment.class)
public interface AssignmentInfo {

    String getId();

    String getTitle();

    Date getBeginTime();

    Date getEndTime();

}
