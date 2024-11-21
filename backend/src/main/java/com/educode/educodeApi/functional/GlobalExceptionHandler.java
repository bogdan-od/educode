package com.educode.educodeApi.functional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальний обробник винятків для всього додатку.
 * Відповідає за централізовану обробку помилок та формування відповідей при виникненні винятків.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обробляє виняток невідповідності типів аргументів методу.
     *
     * @param ex Виняток, що містить інформацію про невідповідність типів
     * @return ResponseEntity з повідомленням про помилку та статусом BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Створюємо Map для зберігання відповіді
        Map<String, String> response = new HashMap<>();

        // Формуємо текст помилки з назвою параметра та його значенням
        String errorText = "Невірний параметр " + ex.getName() + "=" + ex.getValue();
        response.put("error", errorText);

        // Повертаємо відповідь з статусом 400 Bad Request
        return ResponseEntity.badRequest().body(response);
    }
}