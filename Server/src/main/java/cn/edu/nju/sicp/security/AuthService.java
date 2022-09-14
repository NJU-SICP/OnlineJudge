package cn.edu.nju.sicp.security;

import cn.edu.nju.sicp.security.jwt.JwtToken;
import cn.edu.nju.sicp.security.jwt.JwtTokenUtils;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository repository;
    private final AuthenticationManager manager;
    private final JwtTokenUtils utils;
    private final Logger logger;

    public AuthService(UserRepository repository, AuthenticationManager manager, JwtTokenUtils utils) {
        this.repository = repository;
        this.manager = manager;
        this.utils = utils;
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public Authentication authenticate(AbstractAuthenticationToken token) {
        try {
            Authentication authentication = manager.authenticate(token);
            if (authentication.isAuthenticated()) {
                return authentication;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码不正确，请重试。");
        } catch (AuthenticationException e) {
            logger.info(String.format("Authentication Exception: %s", e.getMessage()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户被禁用或锁定，请联系管理员。");
        }
    }

    public JwtToken getJwtToken(User user) {
        String token = utils.createJwtToken(user);
        JwtToken authentication = new JwtToken();
        authentication.setUserId(user.getId());
        authentication.setUsername(user.getUsername());
        authentication.setFullName(user.getFullName());
        authentication.setRoles(user.getRoles());
        authentication.setAuthorities(user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        authentication.setToken(token);
        authentication.setIssued(utils.parseJwtToken(token).getIssuedAt());
        authentication.setExpires(utils.parseJwtToken(token).getExpiration());
        return authentication;
    }

}
