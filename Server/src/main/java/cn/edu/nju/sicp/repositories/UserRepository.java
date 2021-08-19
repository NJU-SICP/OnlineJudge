package cn.edu.nju.sicp.repositories;

import cn.edu.nju.sicp.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    List<User> findFirst5ByUsernameStartingWithOrFullNameStartingWith(String prefix1, String prefix2);

    List<User> findAllByRolesContains(String role);

    Page<User> findAllByIdInAndRolesContains(List<String> userIds, String role, Pageable pageable);

    Page<User> findAllByIdNotInAndRolesContains(List<String> userIds, String role, Pageable pageable);

    long countAllByIdInAndRolesContains(List<String> userIds, String role);

    long countAllByIdNotInAndRolesContains(List<String> userIds, String role);

}
