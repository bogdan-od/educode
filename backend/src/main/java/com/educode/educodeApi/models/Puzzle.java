package com.educode.educodeApi.models;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Клас, що представляє сутність задачі в додатку.
 * Містить основну інформацію про завдання, включаючи назву, опис, вміст, часові обмеження та тестові дані.
 */
@Entity
@Table(name="puzzles")
public class Puzzle {
    // Унікальний ідентифікатор задачі
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Назва задачі
    @Size(min = 5, message = "Назва повинна містити принаймні 5 символів")
    @Size(max = 100, message = "Назва повинна містити максимум 100 символів")
    private String title;

    // Опис завдання задачі
    @Size(min = 5, message = "Опис повинен містити принаймні 5 символів")
    @Size(max = 300, message = "Опис повинен містити максимум 300 символів")
    private String description;

    // Основний контент задачі (умова)
    @Size(min = 20, message = "Контент повинен містити принаймні 20 символів")
    @Size(max = 7000, message = "Контент повинен містити максимум 7000 символів")
    private String content;

    // Користувач, який створив задачу
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Часове обмеження на виконання задачі
    @DecimalMin(value = "0.2", message = "Час повинен бути принаймні 0.2 секунд")
    @DecimalMax(value = "10", message = "Час повинен бути максимум 10 секунд")
    private float timeLimit;

    // Набір тестових даних для перевірки розв'язку
    @Valid
    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PuzzleData> puzzleData = new HashSet<>();

    // Максимальна кількість балів за розв'язання
    @DecimalMax(value = "200.0", inclusive = true, message = "Бали за задачу повинні бути максимум 200")
    private Float score;

    // Набір рішень, наданих користувачами
    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Decision> decisions = new HashSet<>();

    /**
     * Конструктор за замовчуванням для створення нового екземпляру задачі
     */
    public Puzzle() {
    }

    /**
     * Конструктор для створення задачі з усіма параметрами
     * @param id унікальний ідентифікатор
     * @param title назва задачі
     * @param description опис задачі
     * @param content контент задачі
     * @param user користувач-автор
     * @param timeLimit часове обмеження
     * @param puzzleData тестові дані
     * @param score максимальний бал
     * @param decisions рішення користувачів
     */
    public Puzzle(long id, String title, String description, String content, User user, float timeLimit, Set<PuzzleData> puzzleData, Float score, Set<Decision> decisions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.user = user;
        this.timeLimit = timeLimit;
        this.puzzleData = puzzleData;
        this.score = score;
        this.decisions = decisions;
    }

    public Set<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(Set<Decision> decisions) {
        this.decisions = decisions;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(float timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Set<PuzzleData> getPuzzleData() {
        return puzzleData;
    }

    public void setPuzzleData(Set<PuzzleData> puzzleData) {
        this.puzzleData = puzzleData;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }
}
