package com.educode.educodeApi.repositories;

import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.TreeNodeInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreeNodeInvitationRepository extends JpaRepository<TreeNodeInvitation, Long> {

    /**
     * Находит приглашение по коду
     */
    TreeNodeInvitation findByCode(String code);

    /**
     * Находит все приглашения для узла
     */
    List<TreeNodeInvitation> findAllByTreeNode(TreeNode treeNode);

    /**
     * Удаляет все приглашения для узла
     */
    void deleteAllByTreeNode(TreeNode treeNode);

    @EntityGraph(attributePaths = {"users", "roles"})
    Page<TreeNodeInvitation> findByTreeNode(TreeNode treeNode, Pageable pageable);

    Page<TreeNodeInvitation> findByTreeNodeAndActiveTrue(TreeNode treeNode, Pageable pageable);

    int deleteByExpiresAtBefore(LocalDateTime now);

    @EntityGraph(attributePaths = {"treeNode", "roles"})
    @Query("SELECT i FROM TreeNodeInvitation i JOIN i.users u WHERE u.id = :userId")
    Page<TreeNodeInvitation> findAllByUser(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
