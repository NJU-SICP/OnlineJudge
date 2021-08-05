package cn.edu.nju.sicp.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    public JwtAuthorizationFilter(AuthenticationManager manager) {
        super(manager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(JwtTokenUtils.TOKEN_HEADER);
        if (header == null || !header.startsWith(JwtTokenUtils.TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            header = header.substring(JwtTokenUtils.TOKEN_PREFIX.length());
            UsernamePasswordAuthenticationToken token = getAuthenticationToken(header);
            SecurityContextHolder.getContext().setAuthentication(token);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Jwt token has expired.");
            response.getWriter().flush();
            return;
        }
        super.doFilterInternal(request, response, chain);
    }

    private UsernamePasswordAuthenticationToken getAuthenticationToken(String token) throws ExpiredJwtException {
        Claims claims = JwtTokenUtils.parseJwtToken(token);
        String username = claims.getSubject();
        if (username != null) {
            return new UsernamePasswordAuthenticationToken(username, null, null);
        } else {
            return null;
        }
    }
}
