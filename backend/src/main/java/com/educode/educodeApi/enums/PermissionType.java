package com.educode.educodeApi.enums;

import java.util.HashMap;
import java.util.Map;

public enum PermissionType implements CodeEnum<Integer> {
    // === Управління вузлами ===
    CREATE_NODES(1, "Створення вузлів"),
    EDIT_NODES(2, "Редагування вузлів"),
    DELETE_NODES(3, "Видалення вузлів"),
    MOVE_NODES(4, "Переміщення вузлів"),
    VIEW_NODE_MEMBERS(5, "Перегляд учасників вузла"),
    VIEW_NODE_PUZZLES(12, "Перегляд задач вузла"),

    // === Управління групами ===
    CREATE_GROUPS(6, "Створення груп"),
    EDIT_GROUPS(7, "Редагування груп"),
    DELETE_GROUPS(8, "Видалення груп"),
    MOVE_GROUPS(9, "Переміщення груп"),

    // === Управління користувачами ===
    INVITE_USERS(13, "Запрошення користувачів"),
    REMOVE_USERS(14, "Видалення користувачів"),
    ASSIGN_ROLES(15, "Призначення ролей"),
    VIEW_USER_ROLES(16, "Перегляд ролей користувачів"),

    // === Управління домашніми завданнями ===
    CREATE_HOMEWORKS(17, "Створення домашніх завдань"),
    EDIT_HOMEWORKS(18, "Редагування домашніх завдань"),
    DELETE_HOMEWORKS(19, "Видалення домашніх завдань"),
    VIEW_ALL_HOMEWORKS(20, "Перегляд усіх домашніх завдань"),
    VIEW_HOMEWORK_SUBMISSIONS(21, "Перегляд відправлених робіт"),

    // === Управління задачами ===
    CREATE_PUZZLES(23, "Створення задач"),
    DELETE_PUZZLE_FROM_NODE(25, "Від'єднати задачу від вузла"),
    MAKE_PUZZLE_INVISIBLE(26, "Зробити задачу невидимою"),
    MAKE_PUZZLE_VISIBLE(39, "Зробити задачу видимою"),
    ASSIGN_PUZZLE_TO_NODE(37, "Додати задачу до вузла"),
    PUBLISH_PUZZLE(38, "Опублікувати задачу"),
    VIEW_ALL_PUZZLES(40, "Дивитись навіть скриті задачі"),

    // === Управління рішеннями ===
    SUBMIT_DECISIONS(28, "Надсилання рішень"),
    VIEW_HOMEWORK_DECISIONS_CODE(29, "Дивитись код рішень");

    private static final Map<Integer, PermissionType> BY_CODE = new HashMap<>();

    static {
        for (PermissionType t : values()) BY_CODE.put(t.code, t);

        if (values().length != BY_CODE.size()) {
            throw new RuntimeException("Some Permissions have doubled \"code\"");
        }
    }

    private final Integer code;
    private final String description;

    PermissionType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() { return code; }

    public String getDescription() {
        return description;
    }

    public static PermissionType fromCode(Integer code) {
        if (code == null) return null;
        PermissionType t = BY_CODE.get(code);
        if (t == null) throw new IllegalArgumentException("Unknown PermissionType code: " + code);
        return t;
    }

    public String asString() {
        return this.name();
    }
}
