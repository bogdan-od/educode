package com.educode.educodeApi.services;

import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.ForbiddenError;
import com.educode.educodeApi.exceptions.UnauthorizedError;
import com.educode.educodeApi.functional.RequestContext;
import com.educode.educodeApi.lazyinit.UserInclude;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.security.MyUserDetails;
import com.educode.educodeApi.services.base.BaseEntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Сервіс для роботи з користувачами системи.
 * Надає функціонал для автентифікації та отримання даних користувача.
 */
@Service
public class UserService implements BaseEntityService<User, Long> {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RequestContext requestContext;
    @Autowired
    private LazyInitializerService lazyInitializerService;

    /**
     * Отримує поточного автентифікованого користувача.
     * @return User об'єкт автентифікованого користувача або null, якщо користувач не автентифікований
     */
    @Transactional(readOnly = true)
    public User getAuthUser(Set<UserInclude> includes) {
        // Отримуємо дані автентифікації з контексту безпеки
        Authentication auth = requestContext.getAuthentication();

        if (auth == null)
            return null;

        // Конвертуємо дані автентифікації в об'єкт користувача
        MyUserDetails myUserDetails = (MyUserDetails) auth.getPrincipal();
        User user = this.findByLogin(myUserDetails.getUsername(), includes).orElse(null);

        return user;
    }

    public User getAuthUser() {
        return this.getAuthUser(null);
    }

    public User getAuthUserElseThrow(Set<UserInclude> includes, UnauthorizedError unauthorizedError, ForbiddenError forbiddenError, Set<PermissionType> permissions) {
        User user = getAuthUser(includes);

        if (user == null)
            throw unauthorizedError;

        if (permissions != null && !user.hasPermissions(permissions))
            throw forbiddenError;

        return user;
    }

    public User getAuthUserElseThrow() {
        return getAuthUserElseThrow(null, new UnauthorizedError("Ви не авторизовані"), new ForbiddenError("Відмовлено в доступі до цього ресурсу"), null);
    }

    public User getAuthUserElseThrow(Set<PermissionType> permissions) {
        return getAuthUserElseThrow(null, new UnauthorizedError("Ви не авторизовані"), new ForbiddenError("Відмовлено в доступі до цього ресурсу"), permissions);
    }

    public User getAuthUserElseThrow(Set<UserInclude> includes, Set<PermissionType> permissions) {
        return getAuthUserElseThrow(includes, new UnauthorizedError("Ви не авторизовані"), new ForbiddenError("Відмовлено в доступі до цього ресурсу"), permissions);
    }

    /**
     * Перевіряє, чи користувач автентифікований в системі.
     * @return true якщо користувач автентифікований, false - якщо ні
     */
    public Boolean isAuth() {
        return requestContext.getAuthentication() != null && requestContext.getAuthentication().isAuthenticated();
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

    @Transactional(readOnly = true)
    public Optional<User> findByLogin(String login, Set<UserInclude> includes) {
        var user = userRepository.findByLogin(login);
        lazyInitializerService.initialize(user.orElse(null), includes);
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email, Set<UserInclude> includes) {
        var user = userRepository.findByEmail(email);
        lazyInitializerService.initialize(user.orElse(null), includes);
        return user;
    }

    public void throwIfNotAuth() {
        if (!isAuth())
            throw new UnauthorizedError();
    }

    @Override
    public JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    @Override
    public LazyInitializerService getLazyInitializerService() {
        return lazyInitializerService;
    }
}