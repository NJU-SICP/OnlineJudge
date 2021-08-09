package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private ProjectionFactory projectionFactory;

    private final Logger logger;

    public UserController() {
        this.logger = LoggerFactory.getLogger(UserController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<Page<UserInfo>> listUsers(Integer page, Integer size) {
        Page<User> users = repository.findAll(PageRequest.of(page == null ? 0 : page,
                size == null ? 20 : size, Sort.by(Sort.Direction.ASC, "username")));
        return new ResponseEntity<>(users.map(u -> projectionFactory.createProjection(UserInfo.class, u)), HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<List<UserInfo>> searchUsers(String prefix) {
        List<User> users = repository.findFirst5ByUsernameStartingWithOrFullNameStartingWith(prefix, prefix);
        return new ResponseEntity<>(users.stream()
                .map(user -> projectionFactory.createProjection(UserInfo.class, user))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<User> viewUser(@PathVariable String id) {
        User user = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_USER_CREATE)")
    public ResponseEntity<User> createUser(@RequestBody User createdUser) {
        User user = new User();
        user.setUsername(createdUser.getUsername());
        user.setFullName(createdUser.getFullName());
        user.setPassword(createdUser.getPassword());
        user.setRoles(createdUser.getRoles());
        user.setExpires(createdUser.getExpires());
        user.setEnabled(createdUser.isEnabled());
        user.setLocked(createdUser.isLocked());
        repository.save(user);
        logger.info(String.format("CreateUser %s by %s", user, SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_UPDATE)")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        User user = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setFullName(updatedUser.getFullName());
        if (!StringUtils.isEmpty(updatedUser.getPassword())) {
            user.setPassword(updatedUser.getPassword());
        }
        user.setRoles(updatedUser.getRoles());
        user.setExpires(updatedUser.getExpires());
        user.setEnabled(updatedUser.isEnabled());
        user.setLocked(updatedUser.isLocked());
        repository.save(user);
        logger.info(String.format("UpdateUser %s by %s", user, SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_DELETE)")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        User user = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(user);
        logger.info(String.format("DeleteUser %s by %s", user, SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
