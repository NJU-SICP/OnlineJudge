package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(excerptProjection = UserInfo.class)
public interface UserRepository extends MongoRepository<User, String> {

    User findByUsername(String username);

    List<User> findFirst5ByUsernameStartingWithOrFullNameStartingWith(String prefix1, String prefix2);

}
