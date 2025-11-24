package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.PermissionType;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tree_node_members")
public class TreeNodeMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tree_node_id", nullable = false)
    private TreeNode treeNode;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tree_node_member_roles",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, columnDefinition = "BIT(1) DEFAULT 1")
    private boolean canLeave = true;

    public TreeNodeMember() {}

    public TreeNodeMember(User user, TreeNode treeNode, Set<Role> roles) {
        this.user = user;
        this.treeNode = treeNode;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    public TreeNodeMember(User user, TreeNode treeNode, Set<Role> roles, boolean canLeave) {
        this.user = user;
        this.treeNode = treeNode;
        this.roles = roles != null ? roles : new HashSet<>();
        this.canLeave = canLeave;
    }

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public TreeNode getTreeNode() { return treeNode; }
    public void setTreeNode(TreeNode treeNode) { this.treeNode = treeNode; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isCanLeave() {
        return canLeave;
    }

    public void setCanLeave(boolean canLeave) {
        this.canLeave = canLeave;
    }

    // Permission checks
    public boolean hasRole(String roleType) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleType));
    }

    public boolean hasPermission(PermissionType permission) {
        return roles.stream().anyMatch(role -> role.hasPermission(permission));
    }

    public boolean hasAnyPermission(Set<PermissionType> permissions) {
        return roles.stream().anyMatch(role -> role.hasAnyPermission(permissions));
    }

    public Set<PermissionType> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        TreeNodeMember that = (TreeNodeMember) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
