package com.educode.educodeApi.models;

import jakarta.persistence.*;

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
    private String input;

    // Очікувані вихідні дані для перевірки правильності рішення
    private String output;

    // Кількість балів за правильне рішення тесту
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        PuzzleData that = (PuzzleData) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
