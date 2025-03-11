package com.educode.educodeApi.functional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Клас, що містить глобальні змінні для управління аутентифікацією та токенами в додатку.
 */

@Component
@RequestScope
public class RequestContext {
    // Прапорець, що вказує чи токен JWT вичерпав свій термін дії
    private boolean tokenExpired = false;

    // Зберігає поточний JWT токен користувача
    private String jwtToken = null;

    private Authentication authentication;

    public boolean isTokenExpired() {
        return tokenExpired;
    }

    public void setTokenExpired(boolean tokenExpired) {
        this.tokenExpired = tokenExpired;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }
}
