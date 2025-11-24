package com.educode.educodeApi.DTO.puzzle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class PuzzleUpdateDTO {
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

    Long checkerId;

    // Набір тестових даних для перевірки розв'язку
    @Valid
    private Set<PuzzleDataUpdateDTO> puzzleData;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean visible;

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

    public Set<PuzzleDataUpdateDTO> getPuzzleData() {
        return puzzleData;
    }

    public void setPuzzleData(Set<PuzzleDataUpdateDTO> puzzleData) {
        this.puzzleData = puzzleData;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(Long checkerId) {
        this.checkerId = checkerId;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
