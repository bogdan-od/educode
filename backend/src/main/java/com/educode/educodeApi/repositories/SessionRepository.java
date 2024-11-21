package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторій для роботи з сесіями користувачів
 * Забезпечує доступ до операцій CRUD для сутності Session
 */
public interface SessionRepository extends JpaRepository<Session, Long> {
    // Наслідує всі базові методи JpaRepository для роботи з сесіями
}
