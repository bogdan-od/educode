package com.educode.educodeApi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Клас Session представляє сесію користувача в системі.
 * Містить інформацію про користувача, токени доступу та оновлення, а також деталі про пристрій та IP-адресу.
 */
@Entity
@Table(name = "sessions")
public class Session {

    // Унікальний ідентифікатор сесії
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Зв'язок з користувачем, якому належить сесія
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Токен оновлення, пов'язаний з сесією
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    // Токен доступу, пов'язаний з сесією
    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private AccessToken accessToken;

    // Інформація про пристрій
    private String deviceName, deviceType;

    // IP-адреса, з якої створено сесію
    private String ipAddress;

    // Час створення сесії
    @Column(nullable = false, columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;

    /**
     * Метод, що викликається перед збереженням нового об'єкта.
     * Встановлює поточний час як час створення сесії.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Конструктор за замовчуванням.
     */
    public Session() {}

    /**
     * Конструктор з параметрами для створення нової сесії.
     * @param id Унікальний ідентифікатор
     * @param user Користувач
     * @param refreshToken Токен оновлення
     * @param accessToken Токен доступу
     * @param deviceName Назва пристрою
     * @param ipAddress IP-адреса
     * @param deviceType Тип пристрою
     * @param createdAt Час створення
     */
    public Session(Long id, User user, RefreshToken refreshToken, AccessToken accessToken, String deviceName, String ipAddress, String deviceType, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.deviceType = deviceType;
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

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}
