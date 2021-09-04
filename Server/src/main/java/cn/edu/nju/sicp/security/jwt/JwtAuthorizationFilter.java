package cn.edu.nju.sicp.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final UserDetailsService service;
    private final JwtTokenUtils utils;

    public JwtAuthorizationFilter(AuthenticationManager manager,
                                  UserDetailsService service,
                                  JwtTokenUtils utils) {
        super(manager);
        this.service = service;
        this.utils = utils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(JwtTokenUtils.TOKEN_HEADER);
        if (header == null || !header.startsWith(JwtTokenUtils.TOKEN_PREFIX)) {
            chain.doFilter(request, response);
        } else {
            try {
                header = header.substring(JwtTokenUtils.TOKEN_PREFIX.length());
                UsernamePasswordAuthenticationToken token = getAuthenticationToken(header);
                if (token != null) {
                    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                }
                SecurityContextHolder.getContext().setAuthentication(token);
                super.doFilterInternal(request, response, chain);
            } catch (ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Jwt token has expired.");
                response.getWriter().flush();
            }
        }
    }

    private UsernamePasswordAuthenticationToken getAuthenticationToken(String token) throws ExpiredJwtException {
        try {
            Claims claims = utils.parseJwtToken(token);
            String username = claims.getSubject();
            UserDetails userDetails = service.loadUserByUsername(username);
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (Exception e) {
            if (e.getClass().equals(ExpiredJwtException.class)) throw e;
            logger.error(String.format("Cannot verify jwt token: %s", e.getMessage()));
            return null;
        }
    }

}
