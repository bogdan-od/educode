package com.educode.educodeApi.DTO.puzzle;

import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.enums.converters.TaskTypeConverter;
import jakarta.persistence.Convert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;

/**
 * DTO клас для передачі даних про задачу
 */
public class PuzzleCreateDTO {

    // Назва задачі
    @NotNull
    @Size(min = 5, message = "Назва повинна містити принаймні 5 символів")
    @Size(max = 100, message = "Назва повинна містити максимум 100 символів")
    private String title;

    // Опис завдання задачі
    @NotNull
    @Size(min = 5, message = "Опис повинен містити принаймні 5 символів")
    @Size(max = 300, message = "Опис повинен містити максимум 300 символів")
    private String description;

    // Основний контент задачі (умова)
    @NotNull
    @Size(min = 20, message = "Контент повинен містити принаймні 20 символів")
    @Size(max = 7000, message = "Контент повинен містити максимум 7000 символів")
    private String content;

    // Часове обмеження на виконання задачі
    @NotNull
    @DecimalMin(value = "0.2", message = "Час повинен бути принаймні 0.2 секунд")
    @DecimalMax(value = "10", message = "Час повинен бути максимум 10 секунд")
    private Float timeLimit;

    // Максимальна кількість балів за розв'язання
    @NotNull
    @DecimalMax(value = "200.0", inclusive = true, message = "Бали за задачу повинні бути максимум 200")
    private Float score;

    // Набір тестових даних для перевірки розв'язку
    @Valid
    private Set<PuzzleDataCreateDTO> puzzleData;

    Long checkerId;

    // Тип задачі
    @NotNull
    @Convert(converter = TaskTypeConverter.class)
    private TaskType taskType = TaskType.NON_INTERACTIVE;

    @NotNull
    private Boolean visible;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту PuzzleDTO
     */
    public PuzzleCreateDTO() {
    }

    /**
     * Конструктор для створення об'єкту PuzzleDTO з усіма необхідними полями
     * @param title Назва головоломки
     * @param description Опис головоломки
     * @param content Зміст головоломки
     * @param timeLimit Обмеження часу на вирішення
     * @param score Максимальний бал за вирішення
     * @param puzzleData Набір тестових даних для задачі
     */
    public PuzzleCreateDTO(String title, String description, String content, Float timeLimit, Float score, Set<PuzzleDataCreateDTO> puzzleData) {
        this.title = title;
        this.description = description;
        this.content = content;
        this.timeLimit = timeLimit;
        this.score = score;
        this.puzzleData = puzzleData;
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

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Float timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Set<PuzzleDataCreateDTO> getPuzzleData() {
        return puzzleData;
    }

    public void setPuzzleData(Set<PuzzleDataCreateDTO> puzzleData) {
        this.puzzleData = puzzleData;
    }

    public Long getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(Long checkerId) {
        this.checkerId = checkerId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
