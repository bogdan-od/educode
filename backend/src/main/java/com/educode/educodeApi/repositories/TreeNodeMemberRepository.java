package com.educode.educodeApi.repositories;

import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.TreeNodeMember;
import com.educode.educodeApi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TreeNodeMemberRepository extends JpaRepository<TreeNodeMember, Long> {

    /**
     * Проверяет, является ли пользователь участником узла
     */
    boolean existsByUserAndTreeNode(User user, TreeNode treeNode);

    /**
     * Находит связь пользователя с узлом
     */
    TreeNodeMember findByUserAndTreeNode(User user, TreeNode treeNode);

    /**
     * Получает всех участников узла
     */
    Page<TreeNodeMember> findByTreeNode(TreeNode treeNode, Pageable pageable);

    /**
     * Получает все узлы пользователя
     */
    Page<TreeNodeMember> findByUser(User user, Pageable pageable);

    /**
     * Подсчитывает участников с определенной ролью в узле
     */
    @Query("SELECT COUNT(m) FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.treeNode.id = :treeNodeId " +
            "AND r.name = :roleType")
    long countMembersWithRole(@Param("treeNodeId") Long treeNodeId,
                              @Param("roleType") String roleType);

    /**
     * Подсчитывает участников с определенным разрешением в узле
     */
    @Query("SELECT COUNT(m) FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.treeNode.id = :treeNodeId " +
            "AND :permissionType MEMBER OF r.permissions")
    long countMembersWithPermission(@Param("treeNodeId") Long treeNodeId,
                                    @Param("permissionType") PermissionType permissionType);

    /**
     * Подсчитывает участников с любым из указанных разрешений
     */
    @Query("SELECT COUNT(DISTINCT m) FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "JOIN r.permissions p " +
            "WHERE m.treeNode.id = :treeNodeId " +
            "AND p IN :permissionTypes")
    long countMembersWithAnyPermission(@Param("treeNodeId") Long treeNodeId,
                                       @Param("permissionTypes") Collection<PermissionType> permissionTypes);

    /**
     * Получает участников узла с определенной ролью
     */
    @Query("SELECT m FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.treeNode = :treeNode " +
            "AND r.name = :roleType")
    Page<TreeNodeMember> findByTreeNodeAndRole(@Param("treeNode") TreeNode treeNode,
                                               @Param("roleType") String roleType,
                                               Pageable pageable);

    /**
     * Получает участников узла с определенным разрешением
     */
    @Query("SELECT m FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.treeNode = :treeNode " +
            "AND :permissionType MEMBER OF r.permissions")
    Page<TreeNodeMember> findByTreeNodeAndPermission(@Param("treeNode") TreeNode treeNode,
                                                     @Param("permissionType") PermissionType permissionType,
                                                     Pageable pageable);

    /**
     * Получает узлы пользователя с определенной ролью
     */
    @Query("SELECT m FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.user = :user " +
            "AND r.name = :roleType")
    Page<TreeNodeMember> findByUserAndRole(@Param("user") User user,
                                           @Param("roleType") String roleType,
                                           Pageable pageable);

    /**
     * Получает узлы пользователя с определенным разрешением
     */
    @Query("SELECT m FROM TreeNodeMember m " +
            "JOIN m.roles r " +
            "WHERE m.user = :user " +
            "AND :permissionType MEMBER OF r.permissions")
    Page<TreeNodeMember> findByUserAndPermission(@Param("user") User user,
                                                 @Param("permissionType") PermissionType permissionType,
                                                 Pageable pageable);

    /**
     * Удаляет пользователя из всех узлов
     */
    void deleteByUser(User user);

    /**
     * Удаляет всех участников из узла
     */
    void deleteByTreeNode(TreeNode treeNode);

    /**
     * Получает участников только данного узла (не наследованных из родительских)
     */
    @Query("SELECT m FROM TreeNodeMember m WHERE m.treeNode.id = :treeNodeId")
    Page<TreeNodeMember> findDirectMembers(@Param("treeNodeId") Long treeNodeId, Pageable pageable);
}
