package com.educode.educodeApi.functional;

/**
 * Клас, що містить глобальні змінні для управління аутентифікацією та токенами в додатку.
 */
public class GlobalVariables {
    // Прапорець, що вказує чи токен JWT вичерпав свій термін дії
    public static boolean tokenExpired = false;

    // Зберігає поточний JWT токен користувача
    public static String jwtToken = null;
}
