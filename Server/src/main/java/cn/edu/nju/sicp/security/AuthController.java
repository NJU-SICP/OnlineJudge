package cn.edu.nju.sicp.security;

import cn.edu.nju.sicp.security.jwt.JwtToken;
import cn.edu.nju.sicp.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;
    private final Logger logger;

    public AuthController(AuthService service) {
        this.service = service;
        this.logger = LoggerFactory.getLogger(AuthController.class);
    }

    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JwtToken> refresh(@RequestBody LoginRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        logger.info(String.format("Refresh platform=%s %s", request.getPlatform(), user));
        return new ResponseEntity<>(service.getJwtToken(user), HttpStatus.OK);
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

}
