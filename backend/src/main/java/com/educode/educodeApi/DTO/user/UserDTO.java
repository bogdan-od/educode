package com.educode.educodeApi.DTO.user;

import com.educode.educodeApi.DTO.DecisionDTO;
import com.educode.educodeApi.DTO.auth.SessionDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDTO;
import com.educode.educodeApi.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Клас для передачі даних користувача
 */
public class UserDTO {
    private Long id;

    private String name, login, email;

    private Integer rating;

    List<SessionDTO> sessions = new ArrayList<>();

    List<DecisionDTO> decisions = new ArrayList<>();

    List<PuzzleDTO> puzzles = new ArrayList<>();

    /**
      * Конструктор за замовчуванням для створення порожнього об'єкту UserDTO
      */
    public UserDTO() {
    }

    /**
      * Створює новий об'єкт UserDTO з усіма можливими параметрами
      * @param name ім'я користувача
      * @param login логін користувача
      * @param email електронна пошта користувача
      * @param rating рейтинг користувача
      * @param sessions список сесій користувача
      * @param decisions список рішень користувача
      * @param puzzles набір головоломок користувача
      */
    public UserDTO(Long id, String name, String login, String email, Integer rating, List<SessionDTO> sessions, List<DecisionDTO> decisions, List<PuzzleDTO> puzzles) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.email = email;
        this.rating = rating;
        this.sessions = sessions;
        // Перетворення рішень в DTO та сортування за спаданням ID
        this.decisions = decisions;
        // Перетворення задач в DTO та сортування за спаданням ID
        this.puzzles = puzzles;
    }

    /**
      * Створює новий об'єкт UserDTO з базовими параметрами
      * @param name ім'я користувача
      * @param login логін користувача
      * @param email електронна пошта користувача
      * @param rating рейтинг користувача
      */
    public UserDTO(Long id, String name, String login, String email, Integer rating) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.email = email;
        this.rating = rating;
    }

    /**
      * Створює новий об'єкт UserDTO на основі сутності User
      * @param user об'єкт користувача з бази даних
      */
    public UserDTO(User user) {
        this.name = user.getName();
        this.login = user.getLogin();
        this.email = user.getEmail();
        this.rating = user.getRating();
    }

    public List<PuzzleDTO> getPuzzles() {
        return puzzles;
    }

    public void setPuzzles(List<PuzzleDTO> puzzles) {
        this.puzzles = puzzles;
    }

    public List<DecisionDTO> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<DecisionDTO> decisions) {
        this.decisions = decisions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<SessionDTO> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionDTO> sessions) {
        this.sessions = sessions;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
