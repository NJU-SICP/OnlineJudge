package cn.edu.nju.sicp.services;

import cn.edu.nju.sicp.jwt.JwtAuthentication;
import cn.edu.nju.sicp.jwt.JwtTokenUtils;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository repository;
    private final AuthenticationManager manager;
    private final JwtTokenUtils utils;

    public AuthService(UserRepository repository, AuthenticationManager manager, JwtTokenUtils utils) {
        this.repository = repository;
        this.manager = manager;
        this.utils = utils;
    }

    public JwtAuthentication authenticate(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(username, password, new ArrayList<>());
            Authentication authentication = manager.authenticate(token);
            if (authentication.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return getJwtAuthentication((User) authentication.getPrincipal());
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码不正确，请重试。");
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户被禁用或锁定，请联系管理员。");
        }
    }

    public JwtAuthentication getJwtAuthentication(User user) {
        String token = utils.createJwtToken(user);
        JwtAuthentication authentication = new JwtAuthentication();
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

    public void setPassword(String oldPassword, String newPassword) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user.validatePassword(oldPassword)) {
            user.setPassword(newPassword);
            repository.save(user);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "输入的信息不正确，请重试。");
        }
    }

}
