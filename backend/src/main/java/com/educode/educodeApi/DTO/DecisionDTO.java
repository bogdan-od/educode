package com.educode.educodeApi.DTO;

import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.models.User;

import java.time.LocalDateTime;

/**
 * DTO клас для передачі даних про рішення користувача.
 */
public class DecisionDTO {
    private Long id;
    private String language;
    private Float score;
    private boolean isCorrect, isFinished;
    private PuzzleDTO puzzle;
    private LocalDateTime createdAt;
    private UserDTO user;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту DecisionDTO
     */
    public DecisionDTO() {
    }

    /**
     * Конструктор для створення DTO об'єкту з існуючого рішення
     * @param decision об'єкт рішення з якого створюється DTO
     */
    public DecisionDTO(Decision decision) {
        this.id = decision.getId();
        this.language = decision.getLanguage();
        this.score = decision.getScore();
        this.isCorrect = decision.isCorrect();
        this.isFinished = decision.isFinished();
        this.puzzle = new PuzzleDTO(decision.getPuzzle(), true);
        this.createdAt = decision.getCreatedAt();
        this.user = decision.getUser() != null ? new UserDTO(decision.getUser()) : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public PuzzleDTO getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(PuzzleDTO puzzle) {
        this.puzzle = puzzle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }
}