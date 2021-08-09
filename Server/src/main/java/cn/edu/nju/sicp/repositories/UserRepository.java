package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {

    User findByUsername(String username);

    List<User> findFirst5ByUsernameStartingWithOrFullNameStartingWith(String prefix1, String prefix2);

}
