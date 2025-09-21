package com.pidima.gateway.service;

import com.pidima.gateway.config.SecurityProps;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Service
public class JwtService {
    private final SecurityProps props;
    private final SecretKey key;

    public JwtService(SecurityProps props) {
        this.props = props;
        byte[] secretBytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(String subject, Map<String, Object> extraClaims, long expiresInSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(props.getJwt().getIssuer())
                .setAudience(props.getJwt().getAudience())
                .addClaims(extraClaims == null ? Map.of() : extraClaims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject, long expiresInSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(props.getJwt().getIssuer())
                .setAudience(props.getJwt().getAudience())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .claim("typ", "refresh")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
