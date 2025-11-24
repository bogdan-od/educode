package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PuzzleRepository extends JpaRepository<Puzzle, Long> {

    /**
     * Находит все видимые задачи
     */
    Page<Puzzle> findAllByVisibleTrue(Pageable pageable);

    /**
     * Находит задачи, доступные в TreeNode (видимые или принадлежащие узлу)
     */
    @Query("SELECT p FROM Puzzle p " +
            "WHERE p.visible = true OR :treeNode MEMBER OF p.treeNodes " +
            "ORDER BY p.id DESC")
    Page<Puzzle> findByTreeNodeOrVisibleTrue(@Param("treeNode") TreeNode treeNode,
                                             Pageable pageable);

    /**
     * Глобальный поиск задач (только видимые + свои)
     */
    @Query("SELECT p FROM Puzzle p WHERE " +
            "(LOWER(p.title) LIKE %:query% OR LOWER(p.description) LIKE %:query% OR LOWER(p.content) LIKE %:query%) " +
            "AND (p.visible = true OR p.user.id = :userId)")
    Page<Puzzle> searchGlobal(@Param("query") String query,
                              @Param("userId") Long userId,
                              Pageable pageable);

    /**
     * Поиск задач в TreeNode (видимые в узле + свои)
     */
    @Query("SELECT p FROM Puzzle p " +
            "LEFT JOIN p.treeNodes tn " +
            "WHERE (LOWER(p.title) LIKE %:query% OR LOWER(p.description) LIKE %:query% OR LOWER(p.content) LIKE %:query%) " +
            "AND (p.visible = true OR p.user.id = :userId OR tn.id = :treeNodeId)")
    Page<Puzzle> searchInTreeNode(@Param("query") String query,
                                  @Param("treeNodeId") Long treeNodeId,
                                  @Param("userId") Long userId,
                                  Pageable pageable);

    /**
     * Проверяет существование задачи в TreeNode
     */
    boolean existsByTreeNodesContainingAndId(TreeNode treeNode, Long puzzleId);

    @Query("SELECT p FROM Puzzle p JOIN p.treeNodes tn WHERE tn.id = :treeNodeId")
    Page<Puzzle> findDirectPuzzlesByTreeNode(@Param("treeNodeId") Long treeNodeId, Pageable pageable);

    Page<Puzzle> findAllByUserAndVisibleTrue(User user, Pageable pageable);

    Page<Puzzle> findAllByUser(User user, Pageable pageable);
}
