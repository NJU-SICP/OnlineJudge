package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.jwt.JwtTokenUtils;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repository;
    private final AuthenticationManager manager;
    private final JwtTokenUtils utils;
    private final Logger logger;

    public AuthController(UserRepository repository, AuthenticationManager manager,
            JwtTokenUtils utils) {
        this.repository = repository;
        this.manager = manager;
        this.utils = utils;
        this.logger = LoggerFactory.getLogger(AuthController.class);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> userLogin(@RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword(), new ArrayList<>());
        try {
            Authentication authentication = manager.authenticate(token);
            if (authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                logger.info(String.format("UserLogin platform=%s %s", request.getPlatform(), user));
                return new ResponseEntity<>(new LoginResponse(user, utils), HttpStatus.OK);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (BadCredentialsException e) {
            logger.info(String.format("UserLogin platform=%s failed={%s} username={%s}",
                    request.getPlatform(), e.getMessage(), request.getUsername()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码不正确，请重试。");
        } catch (AuthenticationException e) {
            logger.info(String.format("UserLogin platform=%s failed={%s} username={%s}",
                    request.getPlatform(), e.getMessage(), request.getUsername()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户被禁用或锁定，请联系管理员。");
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoginResponse> userLoginRefresh(@RequestBody LoginRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(String.format("UserLogin refresh platform=%s %s", request.getPlatform(), user));
        return new ResponseEntity<>(new LoginResponse(user, utils), HttpStatus.OK);
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> userSetPassword(@RequestBody SetPasswordRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.validatePassword(request.oldPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "输入的信息不正确，请重试。");
        }

        user.setPassword(request.newPassword);
        repository.save(user);
        logger.info(String.format("UserSetPassword %s", user));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    static class LoginRequest {

        private String username;
        private String password;
        private String platform;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

    }

    static class LoginResponse {

        private final String userId;
        private final String username;
        private final String fullName;
        private final Collection<String> roles;
        private final Collection<String> authorities;
        private final String token;
        private final Date issued;
        private final Date expires;

        public LoginResponse(User user, JwtTokenUtils utils) {
            userId = user.getId();
            username = user.getUsername();
            fullName = user.getFullName();
            roles = user.getRoles();
            authorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            token = utils.createJwtToken(user);
            issued = new Date();
            expires = utils.parseJwtToken(token).getExpiration();
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public Collection<String> getRoles() {
            return roles;
        }

        public Collection<String> getAuthorities() {
            return authorities;
        }

        public String getToken() {
            return token;
        }

        public Date getIssued() {
            return issued;
        }

        public Date getExpires() {
            return expires;
        }

    }

    static class SetPasswordRequest {

        private String oldPassword;
        private String newPassword;

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

    }

}
