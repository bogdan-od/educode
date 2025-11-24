package com.educode.educodeApi.DTO.code;

import jakarta.validation.constraints.Size;

/**
 * DTO клас для передачі даних коду та мови на тестування.
 * Використовується для обміну інформацією між клієнтом та сервером при роботі з перевіркою коду.
 */
public class CodeTestDTO {
    @Size(max = 100000, message = "Код не може бути більше за 100000 символів")
    private String code;
    private String language;

    private Long puzzleId;

    private Long homeworkId;
    private Long treeNodeId;

    public Long getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(Long treeNodeId) {
        this.treeNodeId = treeNodeId;
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

    public Long getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(Long puzzleId) {
        this.puzzleId = puzzleId;
    }

    public Long getHomeworkId() {
        return homeworkId;
    }

    public void setHomeworkId(Long homeworkId) {
        this.homeworkId = homeworkId;
    }
}
