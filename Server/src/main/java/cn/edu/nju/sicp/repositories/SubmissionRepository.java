package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.configs.CacheConfig;
import cn.edu.nju.sicp.models.Submission;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

    List<Submission> findAllByResultRetryAtBefore(Date date);

    @Override
    @CacheEvict(cacheNames = CacheConfig.SCORE_CACHE_NAME, key = "#entity.userId + '-' + #entity.assignmentId")
    <S extends Submission> S save(S entity);

    @Override
    @CacheEvict(cacheNames = CacheConfig.SCORE_CACHE_NAME, key = "#entity.userId + '-' + #entity.assignmentId")
    void delete(Submission entity);

}
