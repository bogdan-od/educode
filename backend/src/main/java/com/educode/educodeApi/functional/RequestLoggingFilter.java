package com.educode.educodeApi.functional;

import com.educode.educodeApi.models.User;
import com.educode.educodeApi.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1002)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Map<String, Level> leveledURLs = new HashMap<>();

    {
        leveledURLs.put("/api/notifications/pull", null);
    }

    public RequestLoggingFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (leveledURLs.containsKey(request.getRequestURI()) &&
                    (leveledURLs.get(request.getRequestURI()) == null || !log.isEnabledForLevel(leveledURLs.get(request.getRequestURI())))) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userService.getAuthUser();

            String rand = String.format("%04x", new SecureRandom().nextInt(65536));
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            String requestId = user != null ? String.format("u%d-%s-%s", user.getId(), rand, timestamp) : String.format("guest-%s-%s", rand, timestamp);
            MDC.put("requestId", requestId);

            log.info("Session {} [req = {} {}] [ip = {}] {}{}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    request.getQueryString() != null ? String.format("[params = %s] ", request.getQueryString()) : "",
                    user != null ? (String.format("[user = %d @%s]", user.getId(), user.getLogin())) : "");

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

