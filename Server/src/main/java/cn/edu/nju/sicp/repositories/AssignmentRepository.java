package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.dtos.AssignmentInfo;
import cn.edu.nju.sicp.models.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(excerptProjection = AssignmentInfo.class)
public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    List<Assignment> findAll();

}
