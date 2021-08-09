package cn.edu.nju.sicp.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private final UserDetailsService service;

    public JwtAuthorizationFilter(AuthenticationManager manager, UserDetailsService service) {
        super(manager);
        this.service = service;
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
        Claims claims = JwtTokenUtils.parseJwtToken(token);
        String username = claims.getSubject();
        try {
            UserDetails userDetails = service.loadUserByUsername(username);
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

}
