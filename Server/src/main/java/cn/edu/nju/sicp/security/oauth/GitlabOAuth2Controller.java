package cn.edu.nju.sicp.security.oauth;

import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.security.jwt.JwtToken;
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
public class GitlabOAuth2Controller {

    private final GitlabOAuth2Service service;
    private final Logger logger;

    public GitlabOAuth2Controller(GitlabOAuth2Service service) {
        this.service = service;
        this.logger = LoggerFactory.getLogger(GitlabOAuth2Controller.class);
    }

    @GetMapping("/login")
    public ResponseEntity<Object> login(@RequestParam String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(service.getAuthorizeUri(state));
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @PostMapping("/login/callback")
    public ResponseEntity<JwtToken> authorized(@RequestBody LoginDto dto) {
        GitlabOAuth2Service.GitlabUserInfo info = service.getUserInfo(dto.getCode(), dto.getState());
        User user = service.getRelatedUser(info);
        logger.info(String.format("Login type=gitlab platform=%s user=%s", dto.getPlatform(), user));
        return new ResponseEntity<>(service.getJwtToken(user), HttpStatus.OK);
    }

    @PostMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> link(@RequestBody LoginDto dto) {
        GitlabOAuth2Service.GitlabUserInfo info = service.getUserInfo(dto.getCode(), dto.getState());
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        service.linkRelatedUser(info, user);
        logger.info(String.format("LinkOAuth type=gitlab %s", user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unlink() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        service.unlinkRelatedUser(user);
        logger.info(String.format("UnlinkOAuth type=gitlab %s", user));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    static class LoginDto {

        private String code;
        private String state;
        private String platform;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

    }

}
