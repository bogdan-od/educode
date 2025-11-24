package com.educode.educodeApi.DTO.puzzle;

public class PuzzleDataDTO {
    Long id;

    // Вхідні дані для тестування
    private String input;

    // Очікувані вихідні дані для перевірки правильності рішення
    private String output;

    // Кількість балів за правильне рішення тесту
    private Float score;

    public PuzzleDataDTO() {
    }

    public PuzzleDataDTO(Long id, String input, String output, Float score) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.score = score;
    }

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

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}
