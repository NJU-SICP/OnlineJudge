package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.Token;
import org.springframework.data.mongodb.repository.MongoRepository;

@Deprecated
public interface TokenRepository extends MongoRepository<Token, String> {

}
