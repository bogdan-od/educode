package com.educode.educodeApi.DTO.puzzle;

import com.educode.educodeApi.DTO.DecisionDTO;
import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.enums.converters.TaskTypeConverter;
import jakarta.persistence.Convert;

import java.util.*;

/**
 * DTO клас для передачі даних про задачу
 */
public class PuzzleDTO {
    Long id;

    // Назва задачі
    private String title;

    // Опис завдання задачі
    private String description;

    // Основний контент задачі (умова)
    private String content;

    // Часове обмеження на виконання задачі
    private Float timeLimit;

    // Максимальна кількість балів за розв'язання
    private Float score;

    // Набір тестових даних для перевірки розв'язку
    private List<PuzzleDataDTO> puzzleData;

    private String author;

    private List<DecisionDTO> decisions = new ArrayList<>();

    private Boolean enabled;

    private Long checkerId;

    // Тип задачі
    @Convert(converter = TaskTypeConverter.class)
    private TaskType taskType = TaskType.NON_INTERACTIVE;

    private Boolean visible;

    /**
      * Конструктор за замовчуванням для створення порожнього об'єкту PuzzleDTO
      */
    public PuzzleDTO() {
    }

    /**
      * Конструктор для створення об'єкту PuzzleDTO з усіма необхідними полями
      * @param id Унікальний ідентифікатор головоломки
      * @param title Назва головоломки
      * @param description Опис головоломки
      * @param content Зміст головоломки
      * @param timeLimit Обмеження часу на вирішення
      * @param score Максимальний бал за вирішення
      * @param puzzleData Набір тестових даних для задачі
      * @param author Автор головоломки
      */
    public PuzzleDTO(Long id, String title, String description, String content, Float timeLimit, Float score, List<PuzzleDataDTO> puzzleData, String author, TaskType taskType, List<DecisionDTO> decisions, Boolean enabled, Long checkerId, Boolean visible) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.timeLimit = timeLimit;
        this.score = score;
        this.puzzleData = puzzleData;
        this.author = author;
        this.taskType = taskType;
        this.decisions = decisions;
        this.enabled = enabled;
        this.checkerId = checkerId;
        this.visible = visible;
    }

    //    /**
//      * Конструктор для створення DTO з існуючої моделі Puzzle
//      * @param puzzle Об'єкт задачі для конвертації
//      */
//    public PuzzleDTO(Puzzle puzzle) {
//        // Копіювання основних полів з моделі
//        this.id = puzzle.getId();
//        this.title = puzzle.getTitle();
//        this.description = puzzle.getDescription();
//        this.content = puzzle.getContent();
//        this.timeLimit = puzzle.getTimeLimit();
//        this.score = puzzle.getScore();
//        // Обробка та обмеження тестових даних
//        this.puzzleData = puzzle.getPuzzleData().stream().filter(o -> o.getScore() == 0).sorted(Comparator.comparing(PuzzleData::getId)).map(puzzleData -> {
//            PuzzleData newPuzzleData = new PuzzleData();
//            newPuzzleData.setInput(puzzleData.getInput());
//            newPuzzleData.setOutput(puzzleData.getOutput());
//            newPuzzleData.setScore(puzzleData.getScore());
//            return newPuzzleData;
//        }).collect(Collectors.toSet());
//        this.author = puzzle.getUser().getLogin();
//    }
//
//    /**
//      * Конструктор для створення спрощеної версії DTO з моделі Puzzle, наприклад для відображення в списку задач
//      * @param puzzle Об'єкт задачі для конвертації
//      * @param notFull Прапорець для створення неповної версії DTO
//      */
//    public PuzzleDTO(Puzzle puzzle, boolean notFull) {
//        // Копіювання тільки основних полів для спрощеної версії
//        this.id = puzzle.getId();
//        this.title = puzzle.getTitle();
//        this.description = puzzle.getDescription();
//        this.timeLimit = puzzle.getTimeLimit();
//        this.score = puzzle.getScore();
//        this.author = puzzle.getUser().getLogin();
//    }

    public List<DecisionDTO> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<DecisionDTO> decisions) {
        this.decisions = decisions;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public List<PuzzleDataDTO> getPuzzleData() {
        return puzzleData;
    }

    public void setPuzzleData(List<PuzzleDataDTO> puzzleData) {
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
