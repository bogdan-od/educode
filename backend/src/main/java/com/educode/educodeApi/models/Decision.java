package com.educode.educodeApi.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Клас, що представляє рішення користувача для задачі.
 * Зберігає код, мову програмування, оцінку та інші параметри рішення.
 */
@Entity
@Table(name = "decisions")
public class Decision {
    // Унікальний ідентифікатор рішення
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Код рішення та мова програмування
    @Column(columnDefinition = "TEXT")
    private String code;

    private String language;

    // Оцінка за рішення
    private Float score;

    // Зв'язок з користувачем, який створив рішення
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Зв'язок з задачею, для якої створено рішення
    @ManyToOne
    @JoinColumn(name = "puzzle_id", nullable = false)
    private Puzzle puzzle;

    // Час створення рішення
    @Column(nullable = false, columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime createdAt;

    // Прапорці стану рішення
    private boolean isFinished, isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id")
    private Homework homework;

    /**
     * Метод, що автоматично встановлює час створення рішення перед збереженням у базу даних.
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкта рішення.
     */
    public Decision() {
    }

    /**
     * Конструктор для створення об'єкта рішення з усіма параметрами.
     * @param id Унікальний ідентифікатор
     * @param code Код рішення
     * @param language Мова програмування
     * @param score Оцінка
     * @param user Користувач
     * @param puzzle Задача
     * @param createdAt Час створення
     * @param isFinished Статус завершення
     * @param isCorrect Статус правильності
     */
    public Decision(Long id, String code, String language, Float score, User user, Puzzle puzzle, LocalDateTime createdAt, boolean isFinished, boolean isCorrect) {
        this.id = id;
        this.code = code;
        this.language = language;
        this.score = score;
        this.user = user;
        this.puzzle = puzzle;
        this.createdAt = createdAt;
        this.isFinished = isFinished;
        this.isCorrect = isCorrect;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public Homework getHomework() {
        return homework;
    }

    public void setHomework(Homework homework) {
        this.homework = homework;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Decision that = (Decision) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
