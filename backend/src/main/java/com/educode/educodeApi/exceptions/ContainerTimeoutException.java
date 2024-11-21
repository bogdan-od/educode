package com.educode.educodeApi.exceptions;

/**
 * Виняток, який виникає при перевищенні часу очікування контейнера
 * Використовується для обробки ситуацій, коли виконання коду в контейнері займає більше часу, ніж дозволено
 */
public class ContainerTimeoutException extends Exception {
    /**
     * Створює новий виняток з вказаним повідомленням
     * @param message повідомлення про помилку
     */
    public ContainerTimeoutException(String message) {
        // Викликаємо конструктор батьківського класу з повідомленням
        super(message);
    }

    /**
     * Створює новий виняток з вказаним повідомленням та причиною
     * @param message повідомлення про помилку
     * @param cause причина виникнення винятку
     */
    public ContainerTimeoutException(String message, Throwable cause) {
        // Викликаємо конструктор батьківського класу з повідомленням та причиною
        super(message, cause);
    }
}
