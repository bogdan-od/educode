package com.educode.educodeApi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервіс для роботи з JWT (JSON Web Token) токенами.
 * Забезпечує генерацію, валідацію та обробку JWT токенів для автентифікації користувачів.
 */
@Service
public class JwtUtil {

    // Секретний ключ для підпису JWT токенів
    @Value("${jwt.secret}")
    private String secret;

    // Час життя токена - 2 години в мілісекундах
    private final long tokenExpirationTime = 1000L * 60 * 60 * 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Генерує JWT токен на основі наданих даних користувача.
     * @param username ім'я користувача
     * @param accessToken токен доступу
     * @param roles список ролей користувача
     * @return згенерований JWT токен
     */
    public String generateToken(String username, String accessToken, List<String> roles) {
        // Створюємо Map з даними користувача
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("access_token", accessToken);
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpirationTime))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * Витягує ім'я користувача з JWT токена.
     * @param token JWT токен
     * @return ім'я користувача
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, false);
    }

    /**
     * Витягує список ролей з JWT токена.
     * @param token JWT токен
     * @return список ролей
     */
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class), false);
    }

    /**
     * Витягує токен доступу з JWT токена.
     * @param token JWT токен
     * @return токен доступу
     */
    public String extractAccessToken(String token) {
        return extractClaim(token, claims -> claims.get("access_token", String.class), false);
    }

    /**
     * Витягує токен доступу з JWT токена з можливістю ігнорування закінчення терміну дії.
     * @param token JWT токен
     * @param ignoreExpiration чи ігнорувати закінчення терміну дії
     * @return токен доступу
     */
    public String extractAccessToken(String token, boolean ignoreExpiration) {
        return extractClaim(token, claims -> claims.get("access_token", String.class), ignoreExpiration);
    }

    /**
     * Витягує певне поле з JWT токена.
     * @param token JWT токен
     * @param claimsResolver функція для отримання потрібного поля
     * @param ignoreExpiration чи ігнорувати закінчення терміну дії
     * @return значення поля
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver, boolean ignoreExpiration) {
        final Claims claims = extractAllClaims(token, ignoreExpiration);
        return claimsResolver.apply(claims);
    }

    /**
     * Витягує всі claims з JWT токена.
     * @param token JWT токен
     * @param ignoreExpiration чи ігнорувати закінчення терміну дії
     * @return об'єкт Claims
     */
    private Claims extractAllClaims(String token, boolean ignoreExpiration) {
        try {
            // Якщо ігноруємо термін дії, встановлюємо максимальний час відхилення
            if (ignoreExpiration) {
                return Jwts.parserBuilder()
                        .setSigningKey(secret)
                        .setAllowedClockSkewSeconds(Integer.MAX_VALUE)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            }

            return Jwts.parserBuilder()
                    .setSigningKey(secret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {
            if (ignoreExpiration) {
                return e.getClaims();
            }
            throw e;
        }
    }

    /**
     * Перевіряє, чи закінчився термін дії токена.
     * @param token JWT токен
     * @return true, якщо термін дії закінчився
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Витягує дату закінчення терміну дії з JWT токена.
     * @param token JWT токен
     * @return дата закінчення терміну дії
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration, false);
    }
}
