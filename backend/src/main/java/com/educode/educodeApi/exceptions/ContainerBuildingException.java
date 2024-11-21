package com.educode.educodeApi.exceptions;

/**
 * Клас винятків, що виникають під час побудови контейнера
 */
public class ContainerBuildingException extends Exception {
    // Код помилки
    private int code;

    /**
     * Створює новий виняток з повідомленням та кодом помилки
     * @param message повідомлення про помилку
     * @param code код помилки
     */
    public ContainerBuildingException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * Створює новий виняток з повідомленням та причиною
     * @param message повідомлення про помилку
     * @param cause причина виникнення помилки
     */
    public ContainerBuildingException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getCode() {
        return code;
    }
}
