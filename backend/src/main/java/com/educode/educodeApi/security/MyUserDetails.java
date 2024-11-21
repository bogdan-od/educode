package com.educode.educodeApi.security;

import com.educode.educodeApi.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Клас, що реалізує інтерфейс UserDetails для роботи з Spring Security.
 * Відповідає за надання інформації про користувача для автентифікації та авторизації.
 */
public class MyUserDetails implements UserDetails {
    // Об'єкт користувача, що містить основну інформацію
    private User user;

    /**
     * Конструктор класу MyUserDetails.
     * @param user об'єкт користувача, на основі якого створюється UserDetails
     */
    public MyUserDetails(User user) {
        this.user = user;
    }

    /**
     * Отримує колекцію прав доступу користувача.
     * @return колекція об'єктів GrantedAuthority, що представляють ролі користувача
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Перетворюємо ролі користувача в об'єкти SimpleGrantedAuthority
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    // Інші методи, що реалізують інтерфейс UserDetails

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getLogin();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}