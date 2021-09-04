package cn.edu.nju.sicp.security.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class GitlabOAuth2Beans {

    private final GitlabOAuth2Config config;

    public GitlabOAuth2Beans(GitlabOAuth2Config config) {
        this.config = config;
    }

    @Bean
    public ClientRegistration gitlabOAuth2ClientRegistration() {
        return ClientRegistration.withRegistrationId("gitlab")
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .redirectUri(config.getRedirectUri())
                .scope(config.getScope())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationUri(String.format("%s/oauth/authorize", config.getEndpoint()))
                .tokenUri(String.format("%s/oauth/token", config.getEndpoint()))
                .userInfoUri(String.format("%s/api/v4/user", config.getEndpoint()))
                .userInfoAuthenticationMethod(AuthenticationMethod.HEADER)
                .build();
    }

    @Bean
    public DefaultAuthorizationCodeTokenResponseClient gitlabOAuth2AccessTokenResponseClient() {
        return new DefaultAuthorizationCodeTokenResponseClient();
    }

    @Bean
    public OAuth2AuthorizationCodeAuthenticationProvider gitlabOAuth2AuthenticationProvider
            (DefaultAuthorizationCodeTokenResponseClient gitlabOAuth2AccessTokenResponseClient) {
        return new OAuth2AuthorizationCodeAuthenticationProvider(gitlabOAuth2AccessTokenResponseClient);
    }

}
