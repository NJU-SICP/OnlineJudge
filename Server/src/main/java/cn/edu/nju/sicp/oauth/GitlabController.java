package cn.edu.nju.sicp.oauth;

import cn.edu.nju.sicp.jwt.JwtAuthentication;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/gitlab")
public class GitlabController {

    private final AuthService authService;
    private final GitlabService gitlabService;
    private final Logger logger;

    public GitlabController(AuthService authService, GitlabService gitlabService) {
        this.authService = authService;
        this.gitlabService = gitlabService;
        this.logger = LoggerFactory.getLogger(GitlabController.class);
    }

    @GetMapping("/login")
    public ResponseEntity<Object> login(@RequestParam(defaultValue = "auth") String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(gitlabService.getAuthorizeUri(state));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @PostMapping("/login/callback")
    public ResponseEntity<JwtAuthentication> authorized(@RequestBody LoginRequest request) {
        GitlabService.GitlabOAuthToken token = gitlabService.getOAuthToken(request.getToken());
        GitlabService.GitlabUserInfo info = gitlabService.getUserInfo(token);
        User user = gitlabService.getRelatedUser(info);
        JwtAuthentication authentication = authService.getJwtAuthentication(user);
        logger.info(String.format("Login type=gitlab platform=%s %s", request.getPlatform(), user));
        return new ResponseEntity<>(authentication, HttpStatus.OK);
    }

    @PostMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> link(@RequestBody LoginRequest request) {
        GitlabService.GitlabOAuthToken token = gitlabService.getOAuthToken(request.getToken());
        GitlabService.GitlabUserInfo info = gitlabService.getUserInfo(token);
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        gitlabService.linkRelatedUser(info, user);
        logger.info(String.format("LinkOAuth type=gitlab %s", user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlink() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        gitlabService.unlinkRelatedUser(user);
        logger.info(String.format("UnlinkOAuth type=gitlab %s", user));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    static class LoginRequest {

        private String token;
        private String platform;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

    }

}
