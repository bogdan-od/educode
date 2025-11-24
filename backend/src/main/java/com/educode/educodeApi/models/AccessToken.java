package com.educode.educodeApi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Клас, модель для таблиці в базі даних, що представляє токен доступу.
 * Використовується для аутентифікації та авторизації користувачів.
 */
@Entity
@Table(name = "access_tokens")
public class AccessToken implements BeforeRealUpdate {

    // Унікальний ідентифікатор токену
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Користувач, якому належить токен
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Сесія, пов'язана з токеном
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    // Значення токену
    @Column(nullable = false, unique = true)
    private String token;

    // Дата та час закінчення дії токену
    @Column(nullable = false, columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime validUntil;

    /**
     * Метод, що викликається перед збереженням нового токену.
     * Встановлює термін дії токену на 2 години від поточного часу.
     */
    @PrePersist
    public void prePersist() {
        if (this.validUntil == null) {
            this.validUntil = LocalDateTime.now().plus(2, TimeUnit.HOURS.toChronoUnit());
        }
    }

    /**
     * Метод, що викликається перед оновленням токену.
     * Оновлює термін дії токену на 2 години від поточного часу.
     */
    @Override
    public void preRealUpdate() {
        this.validUntil = LocalDateTime.now().plus(2, TimeUnit.HOURS.toChronoUnit());
    }

    /**
     * Конструктор за замовчуванням.
     */
    public AccessToken() {}

    /**
     * Конструктор з параметрами для створення токену доступу.
     * @param id Унікальний ідентифікатор
     * @param user Користувач
     * @param session Сесія
     * @param token Значення токену
     * @param validUntil Термін дії
     */
    public AccessToken(Long id, User user, Session session, String token, LocalDateTime validUntil) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        AccessToken that = (AccessToken) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}