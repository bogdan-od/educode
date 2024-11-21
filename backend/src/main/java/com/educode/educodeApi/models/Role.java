package com.educode.educodeApi.models;

import jakarta.persistence.*;

/**
 * Клас, що представляє роль користувача в додатку.
 * Використовується для визначення прав доступу та можливостей користувача.
 */
@Entity
@Table(name = "roles")
public class Role {
    // Унікальний ідентифікатор ролі
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Назва ролі
    private String name;

    /**
     * Конструктор за замовчуванням для створення порожнього об'єкту ролі.
     */
    public Role() {}

    /**
     * Конструктор для створення ролі з вказаною назвою.
     * @param name назва ролі
     */
    public Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}