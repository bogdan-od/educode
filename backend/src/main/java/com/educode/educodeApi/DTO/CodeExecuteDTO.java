package com.educode.educodeApi.DTO;

/**
 * Клас DTO для передачі даних про виконання коду
 * Містить інформацію про вхідні дані, мову програмування та код для виконання
 */
public class CodeExecuteDTO {
    private String input, language, code;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}