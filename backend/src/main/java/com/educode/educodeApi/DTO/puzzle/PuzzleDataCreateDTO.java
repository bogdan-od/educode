package com.educode.educodeApi.DTO.puzzle;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PuzzleDataCreateDTO {
    // Вхідні дані для тестування
    @NotNull
    @Size(max = 200, message = "Вхідні дані повинні містити максимум 200 символів")
    private String input;

    // Очікувані вихідні дані для перевірки правильності рішення
    @Size(max = 200, message = "Вихідні дані повинні містити максимум 200 символів")
    private String output;

    // Кількість балів за правильне рішення тесту
    @NotNull
    @DecimalMin(value = "0", inclusive = true, message = "Кількість балів повинна бути не менше 0")
    @DecimalMax(value = "10", inclusive = true, message = "Кількість балів повинна бути максимум 10 балів")
    private Float score;

    public PuzzleDataCreateDTO() {
    }

    public PuzzleDataCreateDTO(String input, String output, Float score) {
        this.input = input;
        this.output = output;
        this.score = score;
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

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}
