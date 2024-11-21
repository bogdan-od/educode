package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Puzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Репозиторій для роботи з задачами (Puzzle)
 * Забезпечує доступ до операцій з базою даних для сутності Puzzle
 */
public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {
    // Наслідує всі стандартні методи JpaRepository для роботи з сутністю Puzzle
}
