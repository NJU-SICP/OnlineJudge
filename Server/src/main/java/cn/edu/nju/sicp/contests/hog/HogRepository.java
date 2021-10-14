package cn.edu.nju.sicp.contests.hog;

import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HogRepository extends MongoRepository<HogEntry, String> {

    List<HogEntry> findByValidIsTrueAndDateBefore(Date date);

}
