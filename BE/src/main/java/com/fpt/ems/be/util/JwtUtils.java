package com.fpt.ems.be.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtils {

    public static final String DEFAULT_SAMPLE_SECRET = "super_secret_jwt_key_must_be_at_least_256_bits_length_for_hs256!";

    private final Environment environment;

    @Value("${app.jwt.secret:super_secret_jwt_key_must_be_at_least_256_bits_length_for_hs256!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-minutes:30}")
    private long expirationMinutes;

    public JwtUtils(Environment environment) {
        this.environment = environment;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validateSecretKey() {
        boolean isDev = environment != null && Arrays.asList(environment.getActiveProfiles()).contains("dev");
        validateJwtSecretConfig(jwtSecret, isDev);
    }

    public static void validateJwtSecretConfig(String secret, boolean isDev) {
        if (!isDev) {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("Cấu hình bảo mật không hợp lệ: JWT_SECRET phải có độ dài tối thiểu 32 byte (256 bits) cho HS256 trong môi trường non-dev.");
            }
            if (DEFAULT_SAMPLE_SECRET.equals(secret)
                    || secret.toLowerCase().contains("example")
                    || secret.toLowerCase().contains("sample")) {
                throw new IllegalStateException("Cấu hình bảo mật không hợp lệ: Không được sử dụng JWT_SECRET mặc định hoặc giá trị mẫu trong môi trường non-dev.");
            }
        }
    }

    public SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 
     */
    public String generateToken(Long userId, String username, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMinutes * 60 * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", roleName)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 
     */
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }

    /**
     * 
     */
    public Claims parseAndValidateClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);

        // Strict algorithm check
        String algorithm = jws.getHeader().getAlgorithm();
        if (!"HS256".equalsIgnoreCase(algorithm)) {
            throw new SecurityException("Thuật toán JWT không hợp lệ: " + algorithm);
        }

        return jws.getPayload();
    }

    /**
     * 
     */
    public Long extractUserId(String token) {
        Claims claims = parseAndValidateClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}
