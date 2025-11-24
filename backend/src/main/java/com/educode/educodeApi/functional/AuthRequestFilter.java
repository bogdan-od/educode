package com.educode.educodeApi.functional;

import com.educode.educodeApi.lazyinit.AccessTokenInclude;
import com.educode.educodeApi.models.AccessToken;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.AccessTokenRepository;
import com.educode.educodeApi.repositories.SessionRepository;
import com.educode.educodeApi.repositories.UserRepository;
import com.educode.educodeApi.security.MyUserDetails;
import com.educode.educodeApi.services.AccessTokenService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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
@Order(1001)
public class AuthRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AccessTokenRepository accessTokenRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final RequestContext requestContext;
    private final AccessTokenService accessTokenService;

    private static final Logger log = LoggerFactory.getLogger(AuthRequestFilter.class);

    /**
     * Конструктор фільтра автентифікації.
     * @param userDetailsService Сервіс для роботи з деталями користувача
     * @param jwtUtil Утиліта для роботи з JWT
     * @param accessTokenRepository Репозиторій токенів доступу
     * @param userRepository Репозиторій користувачів
     * @param sessionRepository Репозиторій сесій
     */
    @Autowired
    public AuthRequestFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil, AccessTokenRepository accessTokenRepository, UserRepository userRepository, SessionRepository sessionRepository, RequestContext requestContext, AccessTokenService accessTokenService) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.requestContext = requestContext;
        this.accessTokenService = accessTokenService;
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

        if (List.of("options").contains(request.getMethod().toLowerCase())) {
            chain.doFilter(request, response);
            return;
        }

        if (List.of("/api/user/auth/refresh-token").contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        log.atDebug()
                .setMessage("Processing authentication request for URI: {} with method: {}")
                .addArgument(request.getRequestURI())
                .addArgument(request.getMethod())
                .log();

        // Отримуємо заголовок авторизації
        final String authorizationHeader = request.getHeader("Authorization");

        log.trace("Authorization header present: {}", authorizationHeader != null);

        String accessTokenString = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            log.debug("Found Bearer token in Authorization header");

            // Витягуємо JWT токен з заголовка
            jwt = authorizationHeader.substring(7);
            requestContext.setJwtToken(jwt);

            log.trace("JWT token extracted and set in request context");

            try {
                // Витягуємо токен доступу з JWT токена
                accessTokenString = jwtUtil.extractAccessToken(jwt);
                log.debug("Access token successfully extracted from JWT");
            } catch (MalformedJwtException e) {
                log.atWarn()
                        .setMessage("Malformed JWT token detected for request: {}")
                        .addArgument(request.getRequestURI())
                        .setCause(e)
                        .log();

                // Якщо JWT токен не правильно сформовано, видаляємо його у користувача
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                chain.doFilter(request, responseWrapper);
                Map<String, Object> responseNewValues = new HashMap<>();
                responseNewValues.put("del_access_token", "1");
                responseNewValues.put("del_refresh_token", "1");

                log.info("Sending token deletion response due to malformed JWT");
                writeToResponse(responseWrapper, response, responseNewValues);
                responseWrapper.copyBodyToResponse();
                return;
            } catch (ExpiredJwtException e) {
                log.atInfo()
                        .setMessage("Expired JWT token detected for request: {}")
                        .addArgument(request.getRequestURI())
                        .setCause(e)
                        .log();

                // Обробка простроченого JWT токена, видаляємо його у користувача
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                requestContext.setTokenExpired(true);

                log.debug("Marked token as expired in request context");

                chain.doFilter(request, responseWrapper);

                if (requestContext.isTokenExpired()) {
                    log.info("Processing expired token cleanup");

                    Map<String, Object> responseNewValues = new HashMap<>();
                    responseNewValues.put("del_access_token", "1");
                    responseNewValues.put("del_refresh_token", "1");
                    Optional<AccessToken> accessToken = accessTokenService.findByToken(jwtUtil.extractAccessToken(jwt, true), Set.of(AccessTokenInclude.SESSION));

                    if (accessToken.isPresent()) {
                        log.debug("Found access token in database, attempting to delete associated session");
                        try {
                            sessionRepository.delete(accessToken.get().getSession());
                            log.info("Successfully deleted session for expired token");
                        } catch (ObjectOptimisticLockingFailureException ignored) {
                            log.warn("Optimistic locking failure when deleting session for expired token");
                        }
                    } else {
                        log.debug("Access token not found in database during expired token cleanup");
                    }

                    writeToResponse(responseWrapper, response, responseNewValues);
                }

                responseWrapper.copyBodyToResponse();

                return;
            }
        } else {
            log.trace("No Bearer token found in Authorization header");
        }

        // Перевірка автентифікації
        Authentication authentication = requestContext.getAuthentication();

        log.trace("Current authentication status: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");

        if (accessTokenString != null && (authentication == null || authentication instanceof AnonymousAuthenticationToken)) {
            log.atDebug()
                    .setMessage("Processing authentication for access token, length: {}")
                    .addArgument(accessTokenString.length())
                    .log();

            Optional<AccessToken> accessToken = accessTokenRepository.findByToken(accessTokenString);
            User myUser = null;

            if (accessToken.isPresent()) {
                myUser = accessToken.get().getUser();
                log.atInfo()
                        .setMessage("Found valid access token for user: {}")
                        .addArgument(myUser != null ? myUser.getLogin() : "unknown")
                        .log();
            } else {
                log.atWarn()
                        .setMessage("Access token not found in database for request: {}")
                        .addArgument(request.getRequestURI())
                        .log();

                // Обробка случаю, коли токен доступу не знайдено в базі даних
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                chain.doFilter(request, responseWrapper);
                Map<String, Object> responseNewValues = new HashMap<>();
                responseNewValues.put("del_access_token", "1");
                responseNewValues.put("del_refresh_token", "1");

                log.info("Sending token deletion response due to invalid access token");
                writeToResponse(responseWrapper, response, responseNewValues);
                responseWrapper.copyBodyToResponse();
                return;
            }

            String username = myUser.getLogin();

            log.debug("Loading user details for username: {}", username);

            // Створення та встановлення автентифікації
            MyUserDetails userDetails = (MyUserDetails) this.userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            requestContext.setAuthentication(usernamePasswordAuthenticationToken);

            log.atInfo()
                    .setMessage("Successfully authenticated user: {} with authorities: {}")
                    .addArgument(username)
                    .addArgument(userDetails.getAuthorities().toString())
                    .log();
        } else if (accessTokenString == null) {
            log.trace("No access token provided, continuing with anonymous authentication");
        } else {
            log.trace("User already authenticated, skipping token processing");
        }

        log.trace("Continuing filter chain for request: {}", request.getRequestURI());
        chain.doFilter(request, response);
        log.trace("Completed filter processing for request: {}", request.getRequestURI());
    }

    /**
     * Записує модифіковану відповідь у HTTP відповідь.
     * @param responseWrapper Обгортка відповіді
     * @param response HTTP відповідь
     * @param newValues Нові значення для додавання у відповідь
     */
    private void writeToResponse(ContentCachingResponseWrapper responseWrapper, HttpServletResponse response, Map<String, Object> newValues) {
        log.atDebug()
                .setMessage("Writing modified response with new values: {}")
                .addArgument(newValues.keySet().toString())
                .log();

        try {
            // Отримуємо контент відповіді
            String responseContent = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            log.trace("Original response content length: {}", responseContent != null ? responseContent.length() : 0);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

            // Перетворюємо відповідь у Map
            Map<Object, Object> responseMap;
            if (responseContent != null && !responseContent.isEmpty()) {
                responseMap = objectMapper.readValue(responseContent, Map.class);
                log.trace("Parsed existing response content to Map with {} entries", responseMap.size());
            } else {
                responseMap = new HashMap<>();
                log.trace("Created new empty response Map");
            }

            // Додаємо нові значення
            responseMap.putAll(newValues);
            log.debug("Added {} new values to response", newValues.size());

            // Записуємо модифіковану відповідь
            String modifiedResponseContent = objectMapper.writeValueAsString(responseMap);
            log.trace("Serialized modified response content, length: {}", modifiedResponseContent.length());

            byte[] responseBytes = modifiedResponseContent.getBytes(StandardCharsets.UTF_8);

            responseWrapper.resetBuffer();
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setContentLength(responseBytes.length);

            outputStream.write(responseBytes);

            outputStream.flush();
            outputStream.close();

            log.info("Successfully wrote modified response with {} bytes", responseBytes.length);
        } catch (IOException e) {
            log.atError()
                    .setMessage("Failed to write modified response")
                    .setCause(e)
                    .log();
            // Ігноруємо помилки вводу/виводу
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }
}
