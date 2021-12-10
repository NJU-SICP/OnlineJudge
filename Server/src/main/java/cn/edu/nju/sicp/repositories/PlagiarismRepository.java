package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.configs.CacheConfig;
import cn.edu.nju.sicp.models.Plagiarism;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlagiarismRepository extends MongoRepository<Plagiarism, String> {

    @Override
    @CacheEvict(cacheNames = CacheConfig.SCORE_CACHE_NAME, key = "#entity.userId + '-' + #entity.assignmentId")
    <S extends Plagiarism> S save(S entity);

    @Override
    @CacheEvict(cacheNames = CacheConfig.SCORE_CACHE_NAME, key = "#entity.userId + '-' + #entity.assignmentId")
    void delete(Plagiarism entity);

}
