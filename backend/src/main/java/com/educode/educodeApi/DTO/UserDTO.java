package com.educode.educodeApi.DTO;

import com.educode.educodeApi.models.Decision;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Клас для передачі даних користувача
 */
public class UserDTO {
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
    public UserDTO(String name, String login, String email, Integer rating, List<SessionDTO> sessions, List<Decision> decisions, Set<Puzzle> puzzles) {
        this.name = name;
        this.login = login;
        this.email = email;
        this.rating = rating;
        this.sessions = sessions;
        // Перетворення рішень в DTO та сортування за спаданням ID
        this.decisions = decisions.stream().map(DecisionDTO::new).sorted(Comparator.comparing(DecisionDTO::getId).reversed()).collect(Collectors.toList());
        // Перетворення задач в DTO та сортування за спаданням ID
        this.puzzles = puzzles.stream().map(PuzzleDTO::new).sorted(Comparator.comparing(PuzzleDTO::getId).reversed()).collect(Collectors.toList());
    }

    /**
      * Створює новий об'єкт UserDTO з базовими параметрами
      * @param name ім'я користувача
      * @param login логін користувача
      * @param email електронна пошта користувача
      * @param rating рейтинг користувача
      */
    public UserDTO(String name, String login, String email, Integer rating) {
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
}
