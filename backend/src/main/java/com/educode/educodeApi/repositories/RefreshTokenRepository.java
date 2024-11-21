package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторій для роботи з токенами оновлення
 * Забезпечує операції збереження та пошуку токенів у базі даних
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Знаходить токен оновлення за його значенням
     * @param token значення токену для пошуку
     * @return Optional, що містить знайдений токен, або пустий Optional якщо токен не знайдено
     */
    Optional<RefreshToken> findByToken(String token);
}