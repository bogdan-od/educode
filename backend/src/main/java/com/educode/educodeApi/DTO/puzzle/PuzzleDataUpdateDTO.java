package com.educode.educodeApi.DTO.puzzle;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PuzzleDataUpdateDTO {
    @NotNull
    Long id;

    // Вхідні дані для тестування
    @NotNull
    @Size(max = 200, message = "Вхідні дані повинні містити максимум 200 символів")
    private String input;

    // Очікувані вихідні дані для перевірки правильності рішення
    @Size(max = 200, message = "Вихідні дані повинні містити максимум 200 символів")
    private String output;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
