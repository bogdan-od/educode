package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторій для роботи з токенами доступу.
 * Забезпечує базові операції CRUD та додаткові методи пошуку токенів.
 */
public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {
    /**
     * Знаходить токен доступу за його значенням.
     * @param token значення токену для пошуку
     * @return Optional, що містить знайдений токен доступу, або пустий Optional якщо токен не знайдено
     */
    Optional<AccessToken> findByToken(String token);
}
