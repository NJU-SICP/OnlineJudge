package cn.edu.nju.sicp.contests.hog;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface HogRepository extends MongoRepository<HogEntry, String> {

}
