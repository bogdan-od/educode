package com.educode.educodeApi.exceptions;

import java.util.Map;

/**
 * Клас винятків для обробки помилок виконання контейнера
 */
public class ContainerExecutionException extends ContainerException {
    public ContainerExecutionException(String output, int exitCode) {
        super("EXECUTION_ERROR",
                "Execution failed, exitCode=" + exitCode,
                "Execution failed: exitCode={}", new Object[]{exitCode},
                Map.of("exitCode", exitCode, "output", output));
    }

    public Integer getCode() {
        return (Integer) getDetails().getOrDefault("exitCode", null);
    }

    public String getOutput() {
        return (String) getDetails().getOrDefault("output", "");
    }
}
