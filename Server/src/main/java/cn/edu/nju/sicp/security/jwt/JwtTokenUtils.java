package cn.edu.nju.sicp.security.jwt;

import cn.edu.nju.sicp.models.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

public class JwtTokenUtils {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer";

    private @Value("${sicp.jwt.issuer}") String JWT_ISSUER;
    private @Value("${sicp.jwt.audience}") String JWT_AUDIENCE;
    private @Value("${sicp.jwt.secret}")  String JWT_SECRET;
    private static final long JWT_EXPIRE = 7 * 24 * 60 * 60;

    public JwtTokenUtils() {
    }

    public String createJwtToken(User user) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().toArray());
        claims.put("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray());
        return Jwts.builder()
                .signWith(key)
                .setClaims(claims)
                .setIssuer(JWT_ISSUER)
                .setAudience(JWT_AUDIENCE)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRE * 1000))
                .compact();
    }

    public Claims parseJwtToken(String token) throws JwtException {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        JwtParser parser = Jwts.parserBuilder()
                .requireIssuer(JWT_ISSUER)
                .requireAudience(JWT_AUDIENCE)
                .setSigningKey(key)
                .build();
        return parser.parseClaimsJws(token).getBody();
    }

}
