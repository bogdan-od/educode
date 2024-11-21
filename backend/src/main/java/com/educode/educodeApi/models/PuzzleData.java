package com.educode.educodeApi.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Клас, що представляє дані для тестування задачі.
 * Містить вхідні дані, очікувані вихідні дані та кількість балів за правильне рішення.
 */
@Entity
@Table(name="puzzle_data")
public class PuzzleData {
    // Унікальний ідентифікатор тестових даних
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Зв'язок з задачею, до якої належать ці тестові дані
    @ManyToOne
    @JoinColumn(name = "puzzle_id", nullable = false)
    private Puzzle puzzle;

    // Вхідні дані для тестування
    @Size(max = 200, message = "Вхідні дані повинні містити максимум 200 символів")
    private String input;

    // Очікувані вихідні дані для перевірки правильності рішення
    @Size(max = 200, message = "Вихідні дані повинні містити максимум 200 символів")
    private String output;

    // Кількість балів за правильне рішення тесту
    @DecimalMin(value = "0.1", inclusive = true, message = "Кількість балів повинна бути принаймні 0.1 бал")
    @DecimalMax(value = "10", inclusive = true, message = "Кількість балів повинна бути максимум 10 балів")
    private Float score;

    /**
     * Конструктор за замовчуванням
     */
    public PuzzleData() {
    }

    /**
     * Конструктор з параметрами для створення нового набору тестових даних
     * @param id унікальний ідентифікатор
     * @param puzzle задача, до якої належать дані
     * @param input вхідні дані для тестування
     * @param output очікувані вихідні дані для перевірки правильності рішення
     * @param score кількість балів за правильне рішення
     */
    public PuzzleData(Long id, Puzzle puzzle, String input, String output, Float score) {
        this.id = id;
        this.puzzle = puzzle;
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

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
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
