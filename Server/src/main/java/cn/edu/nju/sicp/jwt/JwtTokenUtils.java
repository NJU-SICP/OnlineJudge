package cn.edu.nju.sicp.jwt;

import cn.edu.nju.sicp.models.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;

public class JwtTokenUtils {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer";

    private static final String JWT_SECRET = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String JWT_ISSUER = "sicp";
    private static final long JWT_EXPIRE = 7 * 24 * 60 * 60;

    public static String createJwtToken(User user) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        HashMap<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .signWith(key)
                .setClaims(claims)
                .setIssuer(JWT_ISSUER)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRE * 1000))
                .compact();
    }

    public static Claims parseJwtToken(String token) throws JwtException {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        JwtParser parser = Jwts.parserBuilder()
                .requireIssuer(JWT_ISSUER)
                .setSigningKey(key)
                .build();
        return parser.parseClaimsJws(token).getBody();
    }

}
