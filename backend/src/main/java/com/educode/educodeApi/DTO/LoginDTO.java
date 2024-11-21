package com.educode.educodeApi.DTO;

/**
 * DTO клас, що представляє об'єкт для передачі даних про користувача при авторизації.
 */
public class LoginDTO {
    // Логін користувача
    private String login;
    // Пароль користувача
    private String password;
    // Назва та тип пристрою користувача
    private String deviceName, deviceType;

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}