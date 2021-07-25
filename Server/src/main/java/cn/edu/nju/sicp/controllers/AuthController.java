package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.jwt.JwtTokenUtils;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private AuthenticationManager manager;

    private final Logger logger;

    public AuthController() {
        logger = LoggerFactory.getLogger(AuthController.class);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword(), new ArrayList<>());
        try {
            Authentication authentication = manager.authenticate(token);
            if (authentication.isAuthenticated()) {
                User user = (User) authentication.getPrincipal();
                logger.info(String.format("User %s logged in", user.getUsername()));
                LoginResponse response = new LoginResponse(user);
                return new ResponseEntity<>((new ObjectMapper()).writeValueAsString(response), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        } catch (BadCredentialsException e) {
            logger.info(String.format("User %s login failed: %s", request.getUsername(), e.getMessage()));
            return new ResponseEntity<>("账号或密码不正确，请重试。", HttpStatus.FORBIDDEN);
        } catch (AuthenticationException e) {
            logger.info(String.format("User %s login failed: %s", request.getUsername(), e.getMessage()));
            return new ResponseEntity<>("用户被禁用或锁定，请联系管理员。", HttpStatus.FORBIDDEN);
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    static class LoginRequest {

        private String username;
        private String password;

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

    }

    static class LoginResponse {

        private final String username;
        private final String fullName;
        private final int ring;
        private final String token;
        private final Date expires;

        public LoginResponse(User user) {
            username = user.getUsername();
            fullName = user.getFullName();
            ring = user.getRing();
            token = JwtTokenUtils.createJwtToken(user);
            expires = JwtTokenUtils.parseJwtToken(token).getExpiration();
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public int getRing() {
            return ring;
        }

        public String getToken() {
            return token;
        }

        public Date getExpires() {
            return expires;
        }

    }
}
