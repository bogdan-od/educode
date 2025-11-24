package com.educode.educodeApi.models;

import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.enums.converters.TaskTypeConverter;
import jakarta.persistence.*;

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
    private Long id;

    // Назва задачі
    private String title;

    // Опис завдання задачі
    private String description;

    // Основний контент задачі (умова)
    @Column(columnDefinition = "TEXT")
    private String content;

    // Користувач, який створив задачу
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Часове обмеження на виконання задачі
    private Float timeLimit;

    // Набір тестових даних для перевірки розв'язку
    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PuzzleData> puzzleData = new HashSet<>();

    // Максимальна кількість балів за розв'язання
    private Float score;

    // Набір рішень, наданих користувачами
    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Decision> decisions = new HashSet<>();

    // Програма, що перевіряє рішення користувача
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checker_id")
    private Checker checker;

    // Тип задачі
    @Convert(converter = TaskTypeConverter.class)
    @Column(columnDefinition = "TINYINT NOT NULL DEFAULT 0", nullable = false)
    private TaskType taskType = TaskType.NON_INTERACTIVE;

    @Column(columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
    private Boolean enabled = true;

    @Column(columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
    private Boolean visible = true;

    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Homework> homeworks = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "tree_node_puzzles",
            joinColumns = @JoinColumn(name = "puzzle_id"),
            inverseJoinColumns = @JoinColumn(name = "tree_node_id")
    )
    private Set<TreeNode> treeNodes = new HashSet<>();

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
     * @param checker програма, що перевіряє
     * @param taskType тип задачі
     */
    public Puzzle(Long id, String title, String description, String content, User user, Float timeLimit, Set<PuzzleData> puzzleData, Float score, Set<Decision> decisions, Checker checker, TaskType taskType, Boolean enabled, Boolean visible) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.content = content;
        this.user = user;
        this.timeLimit = timeLimit;
        this.puzzleData = puzzleData;
        this.score = score;
        this.decisions = decisions;
        this.checker = checker;
        this.taskType = taskType;
        this.enabled = enabled;
        this.visible = visible;
    }

    public Set<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(Set<Decision> decisions) {
        this.decisions = decisions;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Float timeLimit) {
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

    public Checker getChecker() {
        return checker;
    }

    public void setChecker(Checker checker) {
        this.checker = checker;
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

    public Set<Homework> getHomeworks() {
        return homeworks;
    }

    public void setHomeworks(Set<Homework> homeworks) {
        this.homeworks = homeworks;
    }

    public Set<TreeNode> getTreeNodes() {
        return treeNodes;
    }

    public void setTreeNodes(Set<TreeNode> treeNodes) {
        this.treeNodes = treeNodes;
    }

    public void addTreeNode(TreeNode treeNode) {
        treeNodes.add(treeNode);
        treeNode.getPuzzles().add(this);
    }
    public void removeTreeNode(TreeNode treeNode) {
        treeNodes.remove(treeNode);
        treeNode.getPuzzles().remove(this);
    }

    public void addDecision(Decision decision) {
        decisions.add(decision);
        decision.setPuzzle(this);
    }

    public void removeDecision(Decision decision) {
        decisions.remove(decision);
        decision.setPuzzle(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) return false;
        Puzzle that = (Puzzle) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
