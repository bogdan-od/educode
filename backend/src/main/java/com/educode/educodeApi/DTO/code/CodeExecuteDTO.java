package com.educode.educodeApi.DTO.code;

import jakarta.validation.constraints.Size;

/**
 * Клас DTO для передачі даних про виконання коду
 * Містить інформацію про вхідні дані, мову програмування та код для виконання
 */
public class CodeExecuteDTO {
    @Size(max = 100000, message = "Код не може бути більше за 100000 символів")
    private String code;
    private String language;
    @Size(max = 1000, message = "Вхідні дані не можуть бути більше за 1000 символів")
    private String input;

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