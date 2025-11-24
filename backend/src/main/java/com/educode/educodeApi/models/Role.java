package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.RoleScope;
import com.educode.educodeApi.enums.converters.PermissionConverter;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.enums.converters.RoleScopeConverter;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Клас, що представляє роль користувача в додатку.
 * Використовується для визначення прав доступу та можливостей користувача.
 */
@Entity
@Table(name = "roles")
public class Role {
    // Унікальний ідентифікатор ролі
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Назва ролі
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(targetClass = PermissionType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_type")
    @Convert(converter = PermissionConverter.class)
    private Set<PermissionType> permissions = new HashSet<>();

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long priority;

    @Convert(converter = RoleScopeConverter.class)
    @Column(name = "role_scope")
    private RoleScope scope;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту ролі.
     */
    public Role() {}

    /**
     * Конструктор для створення ролі з вказаною назвою.
     * @param name назва ролі
     */
    public Role(String name) {
        this.name = name;
    }

    public Role(String name, String description, Set<PermissionType> permissions, Long priority, RoleScope scope) {
        this.name = name;
        this.description = description;
        this.permissions = (permissions == null) ? new HashSet<>() : new HashSet<>(permissions);
        this.priority = priority;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<PermissionType> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<PermissionType> permissions) {
        this.permissions = (permissions == null) ? new HashSet<>() : new HashSet<>(permissions);
    }

    public Long getPriority() {
        return priority;
    }

    public void setPriority(Long priority) {
        this.priority = priority;
    }

    public RoleScope getScope() {
        return scope;
    }

    public void setScope(RoleScope scope) {
        this.scope = scope;
    }

    public String asAuthority() {
        return "ROLE_" + name;
    }

    public boolean hasPermission(PermissionType permissionType) {
        return permissions.contains(permissionType);
    }

    public boolean hasAnyPermission(Set<PermissionType> permissionTypes) {
        return permissionTypes.stream().anyMatch(permissions::contains);
    }

    public boolean hasAllPermissions(Set<PermissionType> permissionTypes) {
        return permissions.containsAll(permissionTypes);
    }

    public void addPermission(PermissionType permission) {
        this.permissions.add(permission);
    }

    public void removePermission(PermissionType permission) {
        this.permissions.remove(permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Role that = (Role) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}