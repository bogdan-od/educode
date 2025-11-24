package com.educode.educodeApi.DTO.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserCreateDTO {
    // Ім'я користувача
    @NotBlank(message = "Ім'я не може бути порожнім")
    @Size(min = 3, message = "Ім'я повинно містити принаймні 3 символи")
    @Size(max = 20, message = "Ім'я повинно містити максимум 20 символів")
    private String name;

    // Логін користувача
    @NotBlank(message = "Логін не може бути порожнім")
    @Size(min = 3, message = "Логін повинен містити принаймні 3 символи")
    @Size(max = 20, message = "Логін повинен містити максимум 20 символів")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Логін повинен складатися тільки з латинських букв, цифр і \"_\"")
    private String login;

    // Email користувача
    @NotBlank(message = "Електронна пошта не може бути порожньою")
    @Email(message = "Неправильний формат електронної пошти")
    @Size(max = 50, message = "Електронна пошта повинна містити максимум 50 символів")
    private String email;

    // Пароль
    @NotBlank(message = "Пароль не може бути порожнім")
    @Size(min = 8, message = "Пароль повинен містити принаймні 8 символів")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
