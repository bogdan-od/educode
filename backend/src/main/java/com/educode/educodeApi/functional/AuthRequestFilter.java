package com.educode.educodeApi.functional;

import com.educode.educodeApi.models.AccessToken;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.AccessTokenRepository;
import com.educode.educodeApi.repositories.SessionRepository;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.security.MyUserDetails;
import com.educode.educodeApi.services.JwtUtil;
import com.educode.educodeApi.services.UserDetailsService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Фільтр автентифікації запитів, який перехоплює HTTP-запити та перевіряє JWT токени.
 * Відповідає за обробку автентифікації користувачів та управління сесіями.
 */
@Component
public class AuthRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AccessTokenRepository accessTokenRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    /**
     * Конструктор фільтра автентифікації.
     * @param userDetailsService Сервіс для роботи з деталями користувача
     * @param jwtUtil Утиліта для роботи з JWT
     * @param accessTokenRepository Репозиторій токенів доступу
     * @param userRepository Репозиторій користувачів
     * @param sessionRepository Репозиторій сесій
     */
    @Autowired
    public AuthRequestFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil, AccessTokenRepository accessTokenRepository, UserRepository userRepository, SessionRepository sessionRepository) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Внутрішній метод фільтрації запитів.
     * Перевіряє JWT токен, встановлює автентифікацію та керує сесіями користувачів.
     * @param request HTTP запит
     * @param response HTTP відповідь
     * @param chain Ланцюг фільтрів
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Отримуємо заголовок авторизації
        final String authorizationHeader = request.getHeader("Authorization");

        String accessTokenString = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // Витягуємо JWT токен з заголовка
            jwt = authorizationHeader.substring(7);
            GlobalVariables.jwtToken = jwt;
            try {
                // Витягуємо токен доступу з JWT токена
                accessTokenString = jwtUtil.extractAccessToken(jwt);
            } catch (MalformedJwtException e) {
                // Якщо JWT токен не правильно сформовано, видаляємо його у користувача
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                chain.doFilter(request, responseWrapper);
                Map<String, Object> responseNewValues = new HashMap<>();
                responseNewValues.put("del_access_token", "1");
                responseNewValues.put("del_refresh_token", "1");
                writeToResponse(responseWrapper, response, responseNewValues);
                responseWrapper.copyBodyToResponse();
                return;
            } catch (ExpiredJwtException e) {
                // Обробка простроченого JWT токена, видаляємо його у користувача
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                GlobalVariables.tokenExpired = true;

                chain.doFilter(request, responseWrapper);

                if (GlobalVariables.tokenExpired) {
                    Map<String, Object> responseNewValues = new HashMap<>();
                    responseNewValues.put("del_access_token", "1");
                    responseNewValues.put("del_refresh_token", "1");
                    Optional<AccessToken> accessToken = accessTokenRepository.findByToken(jwtUtil.extractAccessToken(jwt, true));

                    if (accessToken.isPresent()) {
                        sessionRepository.delete(accessToken.get().getSession());
                    }

                    writeToResponse(responseWrapper, response, responseNewValues);
                }

                responseWrapper.copyBodyToResponse();

                return;
            }
        }

        // Перевірка автентифікації
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (accessTokenString != null && (authentication == null || authentication instanceof AnonymousAuthenticationToken)) {
            Optional<AccessToken> accessToken = accessTokenRepository.findByToken(accessTokenString);
            User myUser = null;

            if (accessToken.isPresent())
                myUser = accessToken.get().getUser();
            else {
                // Обробка случаю, коли токен доступу не знайдено в базі даних
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                chain.doFilter(request, responseWrapper);
                Map<String, Object> responseNewValues = new HashMap<>();
                responseNewValues.put("del_access_token", "1");
                responseNewValues.put("del_refresh_token", "1");
                writeToResponse(responseWrapper, response, responseNewValues);
                responseWrapper.copyBodyToResponse();
                return;
            }

            String username = myUser.getLogin();

            // Створення та встановлення автентифікації
            MyUserDetails userDetails = (MyUserDetails) this.userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

            // Створення сесії
            SecurityContext securityContext = SecurityContextHolder.getContext();
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }
        chain.doFilter(request, response);
    }

    /**
     * Записує модифіковану відповідь у HTTP відповідь.
     * @param responseWrapper Обгортка відповіді
     * @param response HTTP відповідь
     * @param newValues Нові значення для додавання у відповідь
     */
    private void writeToResponse(ContentCachingResponseWrapper responseWrapper, HttpServletResponse response, Map<String, Object> newValues) {
        try {
            // Отримуємо контент відповіді
            String responseContent = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

            // Перетворюємо відповідь у Map
            Map<Object, Object> responseMap;
            if (responseContent != null && !responseContent.isEmpty())
                responseMap = objectMapper.readValue(responseContent, Map.class);
            else
                responseMap = new HashMap<>();

            // Додаємо нові значення
            responseMap.putAll(newValues);

            // Записуємо модифіковану відповідь
            String modifiedResponseContent = objectMapper.writeValueAsString(responseMap);

            byte[] responseBytes = modifiedResponseContent.getBytes(StandardCharsets.UTF_8);

            responseWrapper.resetBuffer();
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(responseBytes.length);

            outputStream.write(responseBytes);

            outputStream.flush();
            outputStream.close();
        } catch (IOException ignored) {
            // Ігноруємо помилки вводу/виводу
        }
    }
}
