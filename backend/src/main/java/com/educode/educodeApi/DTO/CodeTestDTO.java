package com.educode.educodeApi.DTO;

/**
 * DTO клас для передачі даних коду та мови на тестування.
 * Використовується для обміну інформацією між клієнтом та сервером при роботі з перевіркою коду.
 */
public class CodeTestDTO {
    private String code, language;

    private Long puzzleId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(Long puzzleId) {
        this.puzzleId = puzzleId;
    }
}
