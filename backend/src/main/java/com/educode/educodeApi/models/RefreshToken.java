package com.educode.educodeApi.models;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Сутність для зберігання токенів оновлення.
 * Використовується для підтримки сесій користувачів та можливості оновлення токенів доступу.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    // Унікальний ідентифікатор токену оновлення
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Користувач, якому належить токен оновлення
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Сесія, пов'язана з токеном оновлення
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    // Значення токену оновлення
    @Column(nullable = false, unique = true)
    private String token;

    // Дата та час, до якого токен є дійсним
    @Column(nullable = false, columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime validUntil;

    /**
     * Метод, що викликається перед збереженням нового токену.
     * Встановлює термін дії токену на 30 днів від поточного часу.
     */
    @PrePersist
    public void prePersist() {
        this.validUntil = LocalDateTime.now().plus(30, TimeUnit.DAYS.toChronoUnit());
    }

    /**
     * Метод, що викликається перед оновленням токену.
     * Оновлює термін дії токену на 30 днів від поточного часу.
     */
    @PreUpdate
    public void preUpdate() {
        this.validUntil = LocalDateTime.now().plus(30, TimeUnit.DAYS.toChronoUnit());
    }

    /**
     * Конструктор за замовчуванням.
     */
    public RefreshToken() {}

    /**
     * Конструктор з параметрами для створення токену оновлення.
     * @param id Унікальний ідентифікатор
     * @param user Користувач
     * @param session Сесія
     * @param token Значення токену
     * @param validUntil Термін дії
     */
    public RefreshToken(Long id, User user, Session session, String token, LocalDateTime validUntil) {
        this.id = id;
        this.user = user;
        this.session = session;
        this.token = token;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }
}