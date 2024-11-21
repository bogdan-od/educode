package com.educode.educodeApi.DTO;

import com.educode.educodeApi.models.Session;

import java.time.LocalDateTime;

/**
 * DTO клас для передачі даних про сесію користувача
 */
public class SessionDTO {
    private Long id;
    private String deviceName, deviceIP, deviceType;

    boolean currentSession;

    LocalDateTime createdAt;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту SessionDTO
     */
    public SessionDTO() {
    }

    /**
     * Конструктор для створення об'єкту SessionDTO з усіма необхідними параметрами
     * @param id Унікальний ідентифікатор сесії
     * @param deviceName Назва пристрою
     * @param deviceIP IP-адреса пристрою
     * @param deviceType Тип пристрою
     * @param createdAt Час створення сесії
     * @param currentSession Прапорець поточної сесії
     */
    public SessionDTO(Long id, String deviceName, String deviceIP, String deviceType, LocalDateTime createdAt, boolean currentSession) {
        this.id = id;
        this.deviceName = deviceName;
        this.deviceIP = deviceIP;
        this.createdAt = createdAt;
        this.deviceType = deviceType;
        this.currentSession = currentSession;
    }

    /**
     * Конструктор для створення об'єкту SessionDTO на основі моделі Session
     * @param session Об'єкт сесії з бази даних
     * @param accessTokenString Токен доступу для порівняння з поточною сесією
     */
    public SessionDTO(Session session, String accessTokenString) {
        this.id = session.getId();
        this.deviceName = session.getDeviceName();
        this.deviceIP = session.getIpAddress();
        this.createdAt = session.getCreatedAt();
        this.deviceType = session.getDeviceType();
        this.currentSession = accessTokenString.equals(session.getAccessToken().getToken());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceIP() {
        return deviceIP;
    }

    public void setDeviceIP(String deviceIP) {
        this.deviceIP = deviceIP;
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

    public boolean isCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(boolean currentSession) {
        this.currentSession = currentSession;
    }
}
