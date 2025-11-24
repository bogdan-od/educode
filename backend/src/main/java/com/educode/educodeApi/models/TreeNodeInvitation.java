package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.InvitationType;
import com.educode.educodeApi.enums.converters.InvitationTypeConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Приглашение для присоединения к TreeNode
 */
@Entity
@Table(name = "tree_node_invitations", indexes = {
    @Index(name = "idx_invitation_code", columnList = "code"),
})
public class TreeNodeInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_node_id", nullable = false)
    private TreeNode treeNode;

    @Column(unique = true, nullable = false)
    private String code;

    // Пользователи, которым предназначено приглашение
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tree_node_invitation_users",
            joinColumns = @JoinColumn(name = "invitation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    // Роли, которые будут назначены при принятии приглашения
    @ManyToMany(fetch = FetchType.EAGER) // Eager для маппера
    @JoinTable(
            name = "tree_node_invitation_roles",
            joinColumns = @JoinColumn(name = "invitation_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    private boolean active = true;

    // ИСПРАВЛЕНО: nullable = true, убран DEFAULT
    @Column(nullable = true)
    private LocalDateTime expiresAt;

    @Convert(converter = InvitationTypeConverter.class)
    @Column(columnDefinition = "TINYINT NOT NULL DEFAULT 0", nullable = false)
    private InvitationType invitationType = InvitationType.PUBLIC;

    // ИСПРАВЛЕНО: FetchType.LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allowed_tree_node_id") // Новое поле
    private TreeNode allowedTreeNode;

    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 1")
    private boolean canLeaveOnJoin = true;

    public TreeNodeInvitation() {}

    public TreeNodeInvitation(TreeNode treeNode, String code, Set<User> users, Set<Role> roles, LocalDateTime expiresAt, InvitationType invitationType, TreeNode allowedTreeNode) {
        this.treeNode = treeNode;
        this.code = code;
        this.users = users != null ? new HashSet<>(users) : new HashSet<>();
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.expiresAt = expiresAt;
        this.invitationType = invitationType != null ? invitationType : InvitationType.PUBLIC;
        this.allowedTreeNode = allowedTreeNode;
    }

    public TreeNodeInvitation(TreeNode treeNode, String code, Set<User> users, Set<Role> roles, LocalDateTime expiresAt, InvitationType invitationType, TreeNode allowedTreeNode, boolean canLeaveOnJoin) {
        this.treeNode = treeNode;
        this.code = code;
        this.users = users != null ? new HashSet<>(users) : new HashSet<>();
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.expiresAt = expiresAt;
        this.invitationType = invitationType != null ? invitationType : InvitationType.PUBLIC;
        this.allowedTreeNode = allowedTreeNode;
        this.canLeaveOnJoin = canLeaveOnJoin;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TreeNode getTreeNode() { return treeNode; }
    public void setTreeNode(TreeNode treeNode) { this.treeNode = treeNode; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isCanLeaveOnJoin() {
        return canLeaveOnJoin;
    }

    public void setCanLeaveOnJoin(boolean canLeaveOnJoin) {
        this.canLeaveOnJoin = canLeaveOnJoin;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public InvitationType getInvitationType() {
        return invitationType;
    }

    public void setInvitationType(InvitationType invitationType) {
        this.invitationType = invitationType;
    }

    public TreeNode getAllowedTreeNode() {
        return allowedTreeNode;
    }

    public void setAllowedTreeNode(TreeNode allowedTreeNode) {
        this.allowedTreeNode = allowedTreeNode;
    }

    public boolean isExpired() {
        // ИСПРАВЛЕНО: Проверяем, что expiresAt не null
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        return active && !isExpired();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        TreeNodeInvitation that = (TreeNodeInvitation) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
