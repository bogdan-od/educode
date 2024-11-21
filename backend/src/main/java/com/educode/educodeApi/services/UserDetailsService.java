package com.educode.educodeApi.services;

import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервіс для роботи з деталями користувачів у системі аутентифікації
 * Реалізує інтерфейс UserDetailsService зі Spring Security
 */
@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {
    @Autowired
    private UserRepository repository;

    /**
     * Завантажує дані користувача за його логіном
     *
     * @param username логін користувача
     * @return об'єкт UserDetails з даними користувача
     * @throws UsernameNotFoundException якщо користувача не знайдено
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Пошук користувача в репозиторії за логіном
        Optional<User> user = repository.findByLogin(username);

        // Перевірка наявності користувача
        if (user.isEmpty())
            throw new UsernameNotFoundException(username + " not found");

        // Конвертація User в MyUserDetails та обробка випадку відсутності користувача
        return user.map(MyUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    }
}