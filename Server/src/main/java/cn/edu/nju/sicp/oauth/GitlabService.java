package cn.edu.nju.sicp.oauth;

import cn.edu.nju.sicp.configs.OAuthConfig;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitlabService {

    // Reference: https://docs.gitlab.com/ee/api/oauth2.html#authorization-code-flow

    private final OAuthConfig config;
    private final UserRepository repository;
    private final OkHttpClient http;
    private final ObjectMapper mapper;
    private final Logger logger;

    public GitlabService(OAuthConfig config, UserRepository repository) {
        this.config = config;
        this.repository = repository;
        this.http = new OkHttpClient();
        this.mapper = new ObjectMapper();
        this.logger = LoggerFactory.getLogger(GitlabService.class);
    }

    public URI getAuthorizeUri(String state) {
        try {
            return new URI(String.format("%s/oauth/authorize?client_id=%s&redirect_uri=%s"
                            + "&response_type=code&state=%s&scope=%s", config.getEndpoint(),
                    config.getClientId(), config.getRedirectUri(), state, config.getScope()));
        } catch (Exception e) {
            logger.debug(String.format("Cannot get gitlab authorize url: %s", e.getMessage()), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法获取外部登录认证地址。");
        }
    }

    public GitlabOAuthToken getOAuthToken(String code) {
        String url = String.format("%s/oauth/token?client_id=%s&client_secret=%s&code=%s" +
                        "&grant_type=authorization_code&redirect_uri=%s", config.getEndpoint(),
                config.getClientId(), config.getClientSecret(), code, config.getRedirectUri());
        RequestBody body = RequestBody.create(new byte[0], null);
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = http.newCall(request).execute()) {
            return mapper.readValue(response.body().string(), GitlabOAuthToken.class);
        } catch (Exception e) {
            logger.debug(String.format("Cannot get access token from gitlab: %s", e.getMessage()), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法从南京大学代码托管服务获取访问凭证。");
        }
    }

    public GitlabUserInfo getUserInfo(GitlabOAuthToken token) {
        String url = String.format("%s/api/v4/user", config.getEndpoint());
        Request request = new Request.Builder()
                .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                .url(url).get().build();
        try (Response response = http.newCall(request).execute()) {
            return mapper.readValue(response.body().string(), GitlabUserInfo.class);
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
                        user.setGitlabUserId(info.getId());
                        user.setGitlabUserEmail(info.getEmail());
                        repository.save(user);
                        return user;
                    } else {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "南京大学代码托管服务提供的账户邮箱不是南京大学的有效邮箱，该地址无法使用外部登录。");
                    }
                });
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
    public static class GitlabOAuthToken {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private long expiresIn;

        @JsonProperty("refresh_token")
        private String refreshToken;

        private String scope;

        @JsonProperty("created_at")
        private long createdAt;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
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
