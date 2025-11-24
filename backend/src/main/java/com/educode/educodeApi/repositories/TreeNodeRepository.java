package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.TreeNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreeNodeRepository extends JpaRepository<TreeNode, Long> {

    /**
     * Находит все корневые узлы (без родителя)
     */
    List<TreeNode> findAllByParentIsNull();

    /**
     * Находит все дочерние узлы
     */
    List<TreeNode> findAllByParent(TreeNode parent);

    /**
     * Проверяет существование узлов с данным родителем
     */
    boolean existsByParent(TreeNode parent);

    // Загружаем узел с участниками, их пользователями и ролями, а также с puzzles
    @Query("""
        select distinct t from TreeNode t
        left join fetch t.members m
        left join fetch m.user u
        left join fetch m.roles r
        left join fetch t.puzzles p
        where t.id = :id
        """)
    Optional<TreeNode> findByIdWithMembersAndPuzzles(@Param("id") Long id);

    // При необходимости: загрузка только для проверки children/count
    @Query("select t from TreeNode t left join fetch t.children where t.id = :id")
    Optional<TreeNode> findByIdWithChildren(@Param("id") Long id);

    // --- НОВЫЕ ОПТИМИЗИРОВАННЫЕ МЕТОДЫ ---

    /**
     * (Task 2) Проверяет, является ли пользователь участником этого узла ИЛИ любого его предка.
     * Использует рекурсивный CTE (нативный SQL).
     * ИСПРАВЛЕНО: Изменена логика JOIN в рекурсивной части для совместимости с H2.
     */
    @Query(value = """
            WITH RECURSIVE Ancestors(id, parent_id) AS (
                -- 1. Начальный узел
                SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
                UNION ALL
                -- 2. Рекурсивный шаг: присоединяем родителя
                SELECT tn.id, tn.parent_id
                FROM Ancestors a 
                JOIN tree_nodes tn ON a.parent_id = tn.id
            )
            -- 3. Проверяем наличие участника в любом из предков
            SELECT EXISTS (
                SELECT 1
                FROM tree_node_members tnm
                WHERE tnm.user_id = :userId
                AND tnm.tree_node_id IN (SELECT id FROM Ancestors)
            )
            """, nativeQuery = true)
    Integer isMemberOfNodeOrAncestors(@Param("startNodeId") Long startNodeId, @Param("userId") Long userId);

    /**
     * (Task 2) Проверяет, имеет ли пользователь право в этом узле ИЛИ любом его предке.
     * Использует рекурсивный CTE (нативный SQL).
     * ИСПРАВЛЕНО: Изменена логика JOIN в рекурсивной части для совместимости с H2.
     */
    @Query(value = """
            WITH RECURSIVE Ancestors(id, parent_id) AS (
                -- 1. Начальный узел
                SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
                UNION ALL
                -- 2. Рекурсивный шаг: присоединяем родителя
                SELECT tn.id, tn.parent_id
                FROM Ancestors a
                JOIN tree_nodes tn ON a.parent_id = tn.id
            )
            -- 3. Проверяем наличие права
            SELECT EXISTS (
                SELECT 1
                FROM tree_node_members tnm
                JOIN tree_node_member_roles tnmr ON tnm.id = tnmr.member_id
                JOIN role_permissions rp ON tnmr.role_id = rp.role_id
                WHERE tnm.user_id = :userId
                AND rp.permission_type = :permissionCode -- Примечание: сравниваем по Integer коду
                AND tnm.tree_node_id IN (SELECT id FROM Ancestors)
            )
            """, nativeQuery = true)
    Integer hasPermissionInNodeOrAncestors(@Param("startNodeId") Long startNodeId,
                                           @Param("userId") Long userId,
                                           @Param("permissionCode") int permissionCode);

    @Query(value = """
        WITH RECURSIVE Ancestors(id, parent_id) AS (
            -- 1. Начальный узел
            SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
            UNION ALL
            -- 2. Рекурсивный шаг: присоединяем родителя
            SELECT tn.id, tn.parent_id
            FROM Ancestors a
            JOIN tree_nodes tn ON a.parent_id = tn.id
        )
        -- 3. Находим максимальный priority
        SELECT COALESCE(MAX(r.priority), 0)
        FROM tree_node_members tnm
        JOIN tree_node_member_roles tnmr ON tnm.id = tnmr.member_id
        JOIN roles r ON tnmr.role_id = r.id
        WHERE tnm.user_id = :userId
        AND tnm.tree_node_id IN (SELECT id FROM Ancestors)
        """, nativeQuery = true)
    Long getMaxRolePriorityInNodeOrAncestors(@Param("startNodeId") Long startNodeId,
                                                @Param("userId") Long userId);

    /**
     * (Task 2) Проверяет, доступен ли ресурс (Puzzle) в этом узле ИЛИ любом его предке.
     * Использует рекурсивный CTE (нативный SQL).
     * ИСПРАВЛЕНО: Изменена логика JOIN в рекурсивной части для совместимости с H2.
     */
    @Query(value = """
            WITH RECURSIVE Ancestors(id, parent_id) AS (
                -- 1. Начальный узел
                SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
                UNION ALL
                -- 2. Рекурсивный шаг: присоединяем родителя
                SELECT tn.id, tn.parent_id
                FROM Ancestors a
                JOIN tree_nodes tn ON a.parent_id = tn.id
            )
            -- 3. Проверяем наличие задачи
            SELECT EXISTS (
                SELECT 1
                FROM tree_node_puzzles tnp
                WHERE tnp.puzzle_id = :puzzleId
                AND tnp.tree_node_id IN (SELECT id FROM Ancestors)
            )
            """, nativeQuery = true)
    Integer hasResourceInNodeOrAncestors(@Param("startNodeId") Long startNodeId, @Param("puzzleId") Long puzzleId);


    // --- НОВЫЙ МЕТОД ДЛЯ ПАГИНАЦИИ (Task 1) ---

    /**
     * (Task 1) Находит все Puzzle, доступные в узле или его предках, с пагинацией.
     * Использует рекурсивный CTE (нативный SQL).
     * ИСПРАВЛЕНО: Обернуто в подзапрос IN() для корректного маппинга Page<Puzzle>.
     */
    @Query(value = """
WITH RECURSIVE Ancestors(id, parent_id) AS (
  SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
  UNION ALL
  SELECT tn.id, tn.parent_id
  FROM Ancestors a
  JOIN tree_nodes tn ON a.parent_id = tn.id
)
SELECT p.id 
FROM puzzles p
WHERE p.id IN (
  SELECT DISTINCT tnp.puzzle_id
  FROM tree_node_puzzles tnp
  JOIN Ancestors a ON tnp.tree_node_id = a.id
)
    """,
            countQuery = """
    WITH RECURSIVE Ancestors(id, parent_id) AS (
      SELECT id, parent_id FROM tree_nodes WHERE id = :startNodeId
      UNION ALL
      SELECT tn.id, tn.parent_id
      FROM Ancestors a
      JOIN tree_nodes tn ON a.parent_id = tn.id
    )
    SELECT COUNT(DISTINCT tnp.puzzle_id)
    FROM tree_node_puzzles tnp
    JOIN Ancestors a ON tnp.tree_node_id = a.id
    """,
            nativeQuery = true)
    Page<Long> findAllAccessiblePuzzles(@Param("startNodeId") Long startNodeId, Pageable pageable);

    @Query(value = """
        WITH RECURSIVE Ancestors AS (
            SELECT id, parent_id, 1 AS level
            FROM tree_nodes 
            WHERE id = :treeNodeId
            UNION ALL
            SELECT tn.id, tn.parent_id, a.level + 1
            FROM Ancestors a
            JOIN tree_nodes tn ON a.parent_id = tn.id
        )
        SELECT id
        FROM Ancestors
        ORDER BY level DESC
        """, nativeQuery = true)
    List<Long> getBreadcrumbIds(@Param("treeNodeId") Long treeNodeId);

    @Query(value = """
    WITH RECURSIVE Ancestors(id, parent_id) AS (
        -- 1. Начальный узел
        SELECT id, parent_id FROM tree_nodes WHERE id = :treeNodeId
        UNION ALL
        -- 2. Рекурсивный шаг: присоединяем родителя
        SELECT tn.id, tn.parent_id
        FROM Ancestors a
        JOIN tree_nodes tn ON a.parent_id = tn.id
    )
    -- 3. Находим уникальных пользователей с фильтрацией по логину
    SELECT DISTINCT u.id
    FROM tree_node_members tnm
    JOIN users u ON tnm.user_id = u.id
    WHERE tnm.tree_node_id IN (SELECT id FROM Ancestors)
    AND LOWER(u.login) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY u.id
    """,
            countQuery = """
    WITH RECURSIVE Ancestors(id, parent_id) AS (
        SELECT id, parent_id FROM tree_nodes WHERE id = :treeNodeId
        UNION ALL
        SELECT tn.id, tn.parent_id
        FROM Ancestors a
        JOIN tree_nodes tn ON a.parent_id = tn.id
    )
    SELECT COUNT(DISTINCT u.id)
    FROM tree_node_members tnm
    JOIN users u ON tnm.user_id = u.id
    WHERE tnm.tree_node_id IN (SELECT id FROM Ancestors)
    AND LOWER(u.login) LIKE LOWER(CONCAT('%', :query, '%'))
    """,
            nativeQuery = true)
    Page<Long> findAccessibleUsersByLogin(@Param("treeNodeId") Long treeNodeId,
                                          @Param("query") String query,
                                          Pageable pageable);
}
