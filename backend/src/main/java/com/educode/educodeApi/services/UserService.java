package com.educode.educodeApi.services;

import com.educode.educodeApi.functional.RequestContext;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Сервіс для роботи з користувачами системи.
 * Надає функціонал для автентифікації та отримання даних користувача.
 */
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RequestContext requestContext;

    /**
     * Отримує поточного автентифікованого користувача.
     * @return User об'єкт автентифікованого користувача або null, якщо користувач не автентифікований
     */
    public User getAuthUser() {
        // Отримуємо дані автентифікації з контексту безпеки
        Authentication auth = requestContext.getAuthentication();

        if (auth == null)
            return null;

        // Конвертуємо дані автентифікації в об'єкт користувача
        MyUserDetails myUserDetails = (MyUserDetails) auth.getPrincipal();
        User user = userRepository.findByLogin(myUserDetails.getUsername()).orElse(null);

        return user;
    }

    /**
     * Перевіряє, чи користувач автентифікований в системі.
     * @return true якщо користувач автентифікований, false - якщо ні
     */
    public Boolean isAuth() {
        return requestContext.getAuthentication() != null;
    }

    /**
     * Отримує токен доступу з глобальних змінних.
     * @return Рядок з токеном доступу або null у випадку помилки
     */
    public String getAccessToken() {
        // Перевіряємо наявність JWT токену
        if (requestContext.getJwtToken() == null) return null;
        try {
            // Витягуємо токен доступу з JWT
            return jwtUtil.extractAccessToken(requestContext.getJwtToken(), true);
        } catch (Exception e) {
            return null;
        }
    }
}