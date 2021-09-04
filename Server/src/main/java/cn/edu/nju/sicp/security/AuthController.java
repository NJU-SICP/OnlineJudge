package cn.edu.nju.sicp.security;

import cn.edu.nju.sicp.security.jwt.JwtToken;
import cn.edu.nju.sicp.models.User;
import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;
    private final Logger logger;

    public AuthController(AuthService service) {
        this.service = service;
        this.logger = LoggerFactory.getLogger(AuthController.class);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtToken> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String platform = request.getPlatform();
        try {
            Authentication authentication = service.authenticate(username, password);
            User user = (User) authentication.getPrincipal();
            logger.info(String.format("Login type=password platform=%s %s", platform, user));
            return new ResponseEntity<>(service.getJwtToken(user), HttpStatus.OK);
        } catch (ResponseStatusException e) {
            logger.info(String.format("Login type=password platform=%s failed={%s} username={%s}",
                    request.getPlatform(), e.getMessage(), request.getUsername()));
            throw e;
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JwtToken> refresh(@RequestBody LoginRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(String.format("Refresh platform=%s %s", request.getPlatform(), user));
        return new ResponseEntity<>(service.getJwtToken(user), HttpStatus.OK);
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> setPassword(@RequestBody SetPasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            service.setPassword(oldPassword, newPassword);
            logger.info(String.format("SetPassword %s", user));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ResponseStatusException e) {
            logger.info(String.format("SetPassword failed={%s} username={%s}", e.getMessage(), user.getUsername()));
            throw e;
        }
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
