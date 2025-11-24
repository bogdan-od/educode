package com.educode.educodeApi.exceptions;

import java.util.Map;

/**
 * Клас винятків, що виникають під час побудови контейнера
 */
public class ContainerBuildingException extends ContainerException {
    /**
     * Створює новий виняток з повідомленням та кодом помилки
     */
    public ContainerBuildingException(String output, int exitCode) {
        super("COMPILE_ERROR",
                "Compilation failed",
                "Compilation failed, exitCode={}", new Object[]{exitCode},
                Map.of("output", output, "exitCode", exitCode));
    }

    public Integer getCode() {
        return (Integer) getDetails().getOrDefault("exitCode", null);
    }

    public String getOutput() {
        return (String) getDetails().getOrDefault("output", "");
    }
}
