package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.models.Homework;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.models.Puzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторій для роботи з рішеннями користувачів
 * Забезпечує доступ до бази даних для сутності Decision
 */
public interface DecisionRepository extends JpaRepository<Decision, Long> {

    /**
     * Знаходить всі рішення конкретного користувача для конкретної задачі
     * @param user користувач, чиї рішення шукаємо
     * @param puzzle задача, для якої шукаємо рішення
     * @return список рішень користувача для вказаної задачі
     */
    @Query("SELECT d FROM Decision d WHERE d.user = :user AND d.puzzle = :puzzle")
    List<Decision> findAllByUserAndPuzzle(@Param("user") User user, @Param("puzzle") Puzzle puzzle);

    Page<Decision> findAllByHomework(Homework homework, Pageable pageable);
}
