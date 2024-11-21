package com.educode.educodeApi.exceptions;

/**
 * Клас винятків для обробки помилок виконання контейнера
 */
public class ContainerExecutionException extends Exception {
    // Код помилки виконання контейнера
    private int code;

    /**
     * Створює новий виняток з повідомленням та кодом помилки
     * @param message повідомлення про помилку
     * @param code код помилки
     */
    public ContainerExecutionException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * Створює новий виняток з повідомленням та причиною
     * @param message повідомлення про помилку
     * @param cause причина виникнення помилки
     */
    public ContainerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getCode() {
        return code;
    }
}
