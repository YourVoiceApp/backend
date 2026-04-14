package com.love.yourvoiceback.common.jwt;

import com.love.yourvoiceback.common.security.AuthenticatedUser;
import com.love.yourvoiceback.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final UserRepository userRepository;
    private final SecretKey secretKey;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtTokenProvider(
            UserRepository userRepository,
            @Value("${auth.jwt.secret}") String base64Secret,
            @Value("${auth.jwt.access-expiration-seconds}") long accessExpirationSeconds,
            @Value("${auth.jwt.refresh-expiration-seconds}") long refreshExpirationSeconds
    ) {
        this.userRepository = userRepository;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String createAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpirationSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        try {
            Long userId = Long.valueOf(claims.getSubject());
            return userRepository.findById(userId)
                    .map(user -> {
                        AuthenticatedUser principal = new AuthenticatedUser(user);
                        return new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );
                    })
                    .orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
