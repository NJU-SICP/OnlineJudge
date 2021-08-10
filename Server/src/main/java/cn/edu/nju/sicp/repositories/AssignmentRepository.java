package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    Page<Assignment> findAllByBeginTimeBefore(Date beginTime, Pageable pageable);

    List<Assignment> findAllByBeginTimeBeforeAndEndTimeBetween(Date beginTime, Date endTime1, Date endTime2);

    List<Assignment> findFirst5ByTitleStartingWith(String prefix);

}
