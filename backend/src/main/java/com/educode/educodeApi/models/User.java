package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.PermissionType;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Клас, що представляє користувача в додатку.
 * Містить основну інформацію про користувача, включаючи особисті дані, облікові дані, рейтинг та зв'язки з іншими сутностями системи.
 */
@Entity
@Table(name = "users")
public class User {
    // Унікальний ідентифікатор користувача
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ім'я користувача
    private String name;

    // Логін користувача
    private String login;

    // Email користувача
    private String email;

    // Пароль
    @Column(name = "password", nullable = false, columnDefinition = "TEXT")
    private String password;

    // Рейтинг користувача
    @Column(name = "rating", columnDefinition = "INT DEFAULT 0")
    private Integer rating = 0;

    // Зв'язок багато-до-багатьох з ролями користувача
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles = new HashSet<>();

    // Токени доступу користувача
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AccessToken> accessTokens = new HashSet<>();

    // Токени оновлення користувача
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    // Сесії користувача
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Session> sessions = new HashSet<>();

    // Рішення користувача
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Decision> decisions = new HashSet<>();

    // Задачі, створені користувачем
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Puzzle> puzzles = new HashSet<>();

    // Програми, що перевіряють рішення користувачів, створені користувачем
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Checker> checkers = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<TreeNodeMember> treeNodeMembers = new HashSet<>();

    /**
     * Конструктор за замовчуванням
     */
    public User() {}

    /**
     * Повний конструктор для створення користувача з усіма параметрами
     * @param id Ідентифікатор користувача
     * @param name Ім'я користувача
     * @param login Логін користувача
     * @param email Електронна пошта користувача
     * @param password Пароль користувача
     * @param rating Рейтинг користувача
     * @param roles Ролі користувача
     * @param accessTokens Токени доступу
     * @param refreshTokens Токени оновлення
     * @param sessions Сесії користувача
     * @param decisions Рішення користувача
     * @param puzzles Задачі користувача
     */
    public User(Long id, String name, String login, String email, String password, Integer rating, Set<Role> roles, Set<AccessToken> accessTokens, Set<RefreshToken> refreshTokens, Set<Session> sessions, Set<Decision> decisions, Set<Puzzle> puzzles, Set<Checker> checkers) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.email = email;
        this.password = password;
        this.rating = rating;
        this.roles = roles;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.sessions = sessions;
        this.decisions = decisions;
        this.puzzles = puzzles;
        this.checkers = checkers;
    }

    public User(String name, String login, String email, String password) {
        this.name = name;
        this.login = login;
        this.email = email;
        this.password = password;
    }

    public Set<Puzzle> getPuzzles() {
        return puzzles;
    }

    public void setPuzzles(Set<Puzzle> puzzles) {
        this.puzzles = puzzles;
    }

    public Set<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(Set<Decision> decisions) {
        this.decisions = decisions;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<AccessToken> getAccessTokens() {
        return accessTokens;
    }

    public void setAccessTokens(Set<AccessToken> accessTokens) {
        this.accessTokens = accessTokens;
    }

    public Set<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }

    public void setRefreshTokens(Set<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    /**
     * Додає нову роль до набору ролей користувача
     * @param role Роль, яку потрібно додати
     */
    public void addToRoles(Role role) {
        this.roles.add(role);
    }

    public Set<Session> getSessions() {
        return sessions;
    }

    public void setSessions(Set<Session> sessions) {
        this.sessions = sessions;
    }

    public Set<Checker> getCheckers() {
        return checkers;
    }

    public void setCheckers(Set<Checker> checkers) {
        this.checkers = checkers;
    }

    public Set<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(Set<Notification> notifications) {
        this.notifications = notifications;
    }

    public boolean hasRole(String roleType) {
        return roles.stream().anyMatch(role -> Objects.equals(role.getName(), roleType));
    }

    public boolean hasRoles(Set<String> roleTypes) {
        return roles.stream().map(Role::getName).collect(Collectors.toSet()).containsAll(roleTypes);
    }

    public boolean hasPermission(PermissionType permission) {
        return roles.stream().anyMatch(role -> role.hasPermission(permission));
    }

    public boolean hasPermissions(Set<PermissionType> permissions) {
        return getAllPermissions().containsAll(permissions);
    }

    public Set<PermissionType> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public Set<TreeNodeMember> getTreeNodeMembers() {
        return treeNodeMembers;
    }

    public void setTreeNodeMembers(Set<TreeNodeMember> treeNodeMembers) {
        this.treeNodeMembers = treeNodeMembers;
    }

    public Set<TreeNode> getTreeNodes() {
        return treeNodeMembers.stream().map(TreeNodeMember::getTreeNode).collect(Collectors.toSet());
    }

    public void addChecker(Checker checker) {
        checkers.add(checker);
        checker.setUser(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        User that = (User) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
