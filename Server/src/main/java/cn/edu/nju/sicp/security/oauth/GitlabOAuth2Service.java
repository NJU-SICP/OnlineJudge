package cn.edu.nju.sicp.security.oauth;

import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import cn.edu.nju.sicp.security.AuthService;
import cn.edu.nju.sicp.security.jwt.JwtToken;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitlabOAuth2Service {

    // Reference: https://docs.gitlab.com/ee/api/oauth2.html#authorization-code-flow

    private final ClientRegistration registration;
    private final UserRepository repository;
    private final AuthService service;
    private final RestTemplate rest;
    private final Logger logger;

    public GitlabOAuth2Service(ClientRegistration registration, UserRepository repository, AuthService service) {
        this.registration = registration;
        this.repository = repository;
        this.service = service;
        this.rest = new RestTemplate();
        this.logger = LoggerFactory.getLogger(GitlabOAuth2Service.class);
    }

    public URI getAuthorizeUri(String state) {
        try {
            OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest
                    .authorizationCode()
                    .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                    .clientId(registration.getClientId())
                    .redirectUri(registration.getRedirectUri())
                    .scopes(registration.getScopes())
                    .state(state)
                    .build();
            return new URI(request.getAuthorizationRequestUri());
        } catch (Exception e) {
            logger.debug(String.format("Cannot get gitlab authorize url: %s", e.getMessage()), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法获取外部登录认证地址。");
        }
    }

    public String getAccessToken(String code, String state) {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误：授权代码不能为空。");
        } else if (state == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误：授权状态不能为空。");
        }
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest
                .authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri())
                .scopes(registration.getScopes())
                .state(state)
                .build();
        OAuth2AuthorizationResponse response = OAuth2AuthorizationResponse
                .success(code)
                .redirectUri(registration.getRedirectUri())
                .state(state)
                .build();
        try {
            OAuth2AuthorizationExchange exchange = new OAuth2AuthorizationExchange(request, response);
            OAuth2AuthorizationCodeAuthenticationToken token =
                    new OAuth2AuthorizationCodeAuthenticationToken(registration, exchange);
            Authentication authentication = service.authenticate(token);
            return (String) authentication.getCredentials(); // access token, not user info
        } catch (Exception e) {
            logger.debug(String.format("Cannot get access token from gitlab: %s", e.getMessage()), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法从南京大学代码托管服务获取访问凭证。");
        }
    }

    public GitlabUserInfo getUserInfo(String code, String state) {
        String url = registration.getProviderDetails().getUserInfoEndpoint().getUri();
        String token = getAccessToken(code, state);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            return rest.exchange(url, HttpMethod.GET, entity, GitlabUserInfo.class).getBody();
        } catch (Exception e) {
            logger.debug(String.format("Cannot get user info from gitlab: %s", e.getMessage()), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法从南京大学代码托管服务获取用户信息。");
        }
    }

    public User getRelatedUser(GitlabUserInfo info) {
        User example = new User();
        example.setGitlabUserId(info.getId());
        return repository.findOne(Example.of(example))
                .orElseGet(() -> {
                    Pattern pattern = Pattern.compile("(.+)@(smail\\.)?nju\\.edu\\.cn");
                    Matcher matcher = pattern.matcher(info.getEmail());
                    if (matcher.matches()) {
                        User user = repository.findByUsername(matcher.group(1))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "南京大学代码托管服务提供的账户邮箱前缀为" + matcher.group(1) +
                                                "，该前缀不是学（工）号或在本系统中不存在。请使用账户密码登录后手动绑定。"));
                        linkRelatedUser(info, user);
                        return user;
                    } else {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "南京大学代码托管服务提供的账户邮箱不是南京大学的有效邮箱，该地址无法使用外部登录。");
                    }
                });
    }

    public JwtToken getJwtToken(User user) {
        return service.getJwtToken(user);
    }

    public void linkRelatedUser(GitlabUserInfo info, User user) {
        User example = new User();
        example.setGitlabUserId(info.getId());
        User current = repository.findOne(Example.of(example)).orElse(null);
        if (current != null && !Objects.equals(user.getId(), current.getId())) {
            logger.debug(String.format("Gitlab account %d already linked to %s", info.getId(), current));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "南京大学代码托管服务提供的账户已被其他用户绑定。");
        } else if (user.getGitlabUserId() == null) {
            user.setGitlabUserId(info.getId());
            user.setGitlabUserEmail(info.getEmail());
            repository.save(user);
        }
    }

    public void unlinkRelatedUser(User user) {
        if (user.getGitlabUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户没有绑定南京大学南京大学代码托管服务账户。");
        } else {
            user.setGitlabUserId(null);
            user.setGitlabUserEmail(null);
            repository.save(user);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GitlabUserInfo {

        private long id;
        private String email;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

    }

}
