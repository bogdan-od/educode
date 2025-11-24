package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.models.Group;
import com.educode.educodeApi.models.Homework;
import com.educode.educodeApi.models.Puzzle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    /**
     * Находит домашние задания по группе, отсортированные по дате создания
     *
     * @param group группа
     * @param pageable параметры пагинации
     * @return страница домашних заданий
     */
    @EntityGraph(attributePaths = {"puzzle"})
    Page<Homework> findByGroupOrderByCreatedAtDesc(Group group, Pageable pageable);

    /**
     * Находит домашние задания по группе, которые еще не просрочены
     *
     * @param group группа
     * @param pageable параметры пагинации
     * @return страница домашних заданий
     */
    @EntityGraph(attributePaths = {"puzzle"})
    @Query("SELECT h FROM Homework h WHERE h.group = :group AND (h.deadline IS NULL OR h.deadline > CURRENT_TIMESTAMP) ORDER BY h.createdAt DESC")
    Page<Homework> findByGroupAndDeadlineAfterNowOrderByCreatedAtDesc(@Param("group") Group group, Pageable pageable);

    /**
     * Находит домашние задания пользователя на основе его участия в группах через TreeNode
     *
     * Логика: Пользователь участвует в группе, если он участвует в TreeNode этой группы
     *
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return страница домашних заданий
     */
    @Query("SELECT h FROM Homework h " +
            "JOIN h.group g " +
            "JOIN g.treeNode tn " +
            "JOIN tn.members member " +
            "WHERE member.user.id = :userId " +
            "ORDER BY h.createdAt DESC")
    @EntityGraph(attributePaths = {"group", "puzzle"})
    Page<Homework> findHomeworksByUserMembership(@Param("userId") Long userId, Pageable pageable);

    /**
     * Находит активные домашние задания пользователя на основе его участия в группах через TreeNode
     *
     * Активные = deadline еще не прошел (или вообще нет deadline'а)
     *
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return страница активных домашних заданий
     */
    @Query("SELECT h FROM Homework h " +
            "JOIN h.group g " +
            "JOIN g.treeNode tn " +
            "JOIN tn.members member " +
            "WHERE member.user.id = :userId " +
            "AND (h.deadline IS NULL OR h.deadline > CURRENT_TIMESTAMP) " +
            "ORDER BY h.createdAt DESC")
    @EntityGraph(attributePaths = {"group", "puzzle"})
    Page<Homework> findActiveHomeworksByUserMembership(@Param("userId") Long userId, Pageable pageable);

    /**
     * Проверяет, сдал ли пользователь домашнее задание (есть ли хотя бы одно решение)
     *
     * @param homeworkId ID домашнего задания
     * @param userId ID пользователя
     * @return true если пользователь сдал задание
     */
    @Query("SELECT COUNT(d) > 0 FROM Decision d " +
            "WHERE d.homework.id = :homeworkId " +
            "AND d.user.id = :userId")
    boolean existsSubmissionByHomeworkAndUser(@Param("homeworkId") Long homeworkId, @Param("userId") Long userId);

    /**
     * Подсчитывает количество решений для домашнего задания
     *
     * @param homeworkId ID домашнего задания
     * @return количество решений
     */
    @Query("SELECT COUNT(d) FROM Decision d WHERE d.homework.id = :homeworkId")
    long countSubmissionsByHomework(@Param("homeworkId") Long homeworkId);

    /**
     * Находит домашние задания по группе и пользователю (для проверки доступа)
     *
     * Используется чтобы проверить что пользователь участвует в группе И получить домашние задания
     *
     * @param group группа
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return страница домашних заданий
     */
    @Query("SELECT h FROM Homework h " +
            "JOIN h.group g " +
            "JOIN g.treeNode tn " +
            "JOIN tn.members member " +
            "WHERE h.group = :group " +
            "AND member.user.id = :userId " +
            "ORDER BY h.createdAt DESC")
    Page<Homework> findByGroupAndUserMembership(@Param("group") Group group,
                                                @Param("userId") Long userId,
                                                Pageable pageable);

    /**
     * Находит домашние задания с определенной задачей
     *
     * @param puzzleId ID задачи
     * @param pageable параметры пагинации
     * @return страница домашних заданий
     */
    @Query("SELECT h FROM Homework h WHERE h.puzzle.id = :puzzleId ORDER BY h.createdAt DESC")
    Page<Homework> findByPuzzleId(@Param("puzzleId") Long puzzleId, Pageable pageable);

    /**
     * Находит домашние задания с определенной задачей, deadline которых еще не прошел
     *
     * Используется при скрытии задачи (MAKE_PUZZLE_INVISIBLE) чтобы проверить
     * нельзя ли скрыть задачу если на нее еще есть активные домашние задания
     *
     * @param puzzle задача
     * @return множество активных домашних заданий с этой задачей
     */
    @EntityGraph(attributePaths = {"group"})
    @Query("SELECT h FROM Homework h WHERE h.puzzle = :puzzle AND h.deadline > CURRENT_TIMESTAMP")
    Set<Homework> findByPuzzleAndDeadlineAfterNow(@Param("puzzle") Puzzle puzzle);

    /**
     * Подсчитывает количество домашних заданий с определенной задачей
     *
     * Используется при удалении задачи чтобы проверить есть ли на нее активные домашние задания
     *
     * @param puzzle задача
     * @return количество домашних заданий
     */
    long countByPuzzle(Puzzle puzzle);
}
