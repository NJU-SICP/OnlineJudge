package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {

    Optional<Assignment> findOneByIdOrSlug(String id, String slug);

    Page<Assignment> findAllByBeginTimeBefore(Date beginTime, Pageable pageable);

    List<Assignment> findAllByBeginTimeBeforeAndEndTimeBetween(Date beginTime, Date endTime1, Date endTime2);

    List<Assignment> findFirst5ByTitleStartingWithOrSlugStartingWith(String prefix1, String prefix2);

}
