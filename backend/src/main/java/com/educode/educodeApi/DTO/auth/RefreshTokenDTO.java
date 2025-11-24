package com.educode.educodeApi.DTO.auth;

/**
 * Клас DTO для оновлення токенів доступу та оновлення.
 */
public class RefreshTokenDTO {
    // Поля для зберігання токену оновлення, токену доступу та назви пристрою
    private String refreshToken, accessToken, deviceName;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
