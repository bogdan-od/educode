package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Репозиторій для роботи з користувачами в базі даних.
 * Надає методи для пошуку та управління користувачами.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Знаходить користувача за логіном
     * @param login логін користувача для пошуку
     * @return Optional з знайденим користувачем або пустий Optional якщо користувач не знайдений
     */
    Optional<User> findByLogin(String login);

    /**
     * Знаходить користувача за електронною поштою
     * @param email електронна пошта користувача для пошуку
     * @return Optional з знайденим користувачем або пустий Optional якщо користувач не знайдений
     */
    Optional<User> findByEmail(String email);

    /**
     * Пошук користувачів за логіном
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.login) LIKE %:query%")
    Page<User> searchByLogin(@Param("query") String query, Pageable pageable);
}