package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repository;

    private final Logger logger;

    public UserController() {
        this.logger = LoggerFactory.getLogger(UserController.class);
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(String prefix) {
        List<User> users = repository.findFirst5ByUsernameStartingWithOrFullNameStartingWith(prefix, prefix);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

}
