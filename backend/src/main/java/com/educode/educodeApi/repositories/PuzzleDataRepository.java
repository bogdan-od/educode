package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.PuzzleData;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторій для роботи з тестовими даними головоломок (PuzzleData)
 * Забезпечує базові операції CRUD через JpaRepository
 */
public interface PuzzleDataRepository extends JpaRepository<PuzzleData, Long> {
    // Наслідує всі стандартні методи JpaRepository для роботи з сутністю PuzzleData
}
