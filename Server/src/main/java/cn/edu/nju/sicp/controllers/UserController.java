package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.dtos.UserInfo;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final ProjectionFactory projectionFactory;
    private final Logger logger;

    public UserController(UserRepository userRepository, ProjectionFactory projectionFactory) {
        this.userRepository = userRepository;
        this.projectionFactory = projectionFactory;
        this.logger = LoggerFactory.getLogger(UserController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<Page<UserInfo>> listUsers(@RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        Page<UserInfo> infos = userRepository
                .findAll(PageRequest.of(page == null || page < 0 ? 0 : page,
                        size == null || size < 0 ? 20 : size,
                        Sort.by(Sort.Direction.ASC, "username")))
                .map(u -> projectionFactory.createProjection(UserInfo.class, u));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<List<UserInfo>> listAllUsers() {
        List<UserInfo> infos = userRepository
                .findAll(Sort.by(Sort.Direction.ASC, "username")).stream()
                .map(u -> projectionFactory.createProjection(UserInfo.class, u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<List<UserInfo>> searchUsers(@RequestParam String prefix) {
        List<User> users = userRepository.findFirst5ByUsernameStartingWithOrFullNameStartingWith(prefix, prefix);
        return new ResponseEntity<>(users.stream()
                .map(user -> projectionFactory.createProjection(UserInfo.class, user))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_READ)")
    public ResponseEntity<User> viewUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.clearPassword();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/self")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> viewSelf() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        user.clearPassword();
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_USER_CREATE)")
    public ResponseEntity<User> createUser(@RequestBody User createdUser) {
        User user = userRepository.findByUsername(createdUser.getUsername())
            .orElseGet(() -> {
                User u = new User();
                u.setValues(createdUser);
                userRepository.save(u);
                logger.info(String.format("CreateUser %s", u));
                return u;
            });
        user.clearPassword();
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_CREATE)")
    public ResponseEntity<List<UserInfo>> importUsers(@RequestBody List<User> createdUsers) {
        List<User> users = new ArrayList<>();
        for (User createdUser : createdUsers) {
            User user = userRepository.findByUsername(createdUser.getUsername())
                    .orElseGet(() -> {
                        User u = new User();
                        u.setValues(createdUser);
                        userRepository.save(u);
                        return u;
                    });
            users.add(user);
        }
        List<UserInfo> infos = users.stream()
                .map(u -> projectionFactory.createProjection(UserInfo.class, u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_UPDATE)")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!user.getRoles().contains(RolesConfig.ROLE_ADMIN) &&
                updatedUser.getRoles().contains(RolesConfig.ROLE_ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不允许通过API赋予管理员权限。");
        }
        user.setValues(updatedUser);
        userRepository.save(user);
        user.clearPassword();
        logger.info(String.format("UpdateUser %s %s", user, SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_USER_DELETE)")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userRepository.delete(user);
        logger.info(String.format("DeleteUser %s %s", user, SecurityContextHolder.getContext().getAuthentication().getPrincipal()));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
