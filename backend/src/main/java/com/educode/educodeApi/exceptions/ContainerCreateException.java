package com.educode.educodeApi.exceptions;

import java.util.Map;

public class ContainerCreateException extends ContainerException {
    public ContainerCreateException(String output, int exitCode) {
        super("CREATION_ERROR",
                "Container creation failed, exitCode=" + exitCode,
                "Container creation failed, exitCode={}, output={}", new Object[]{exitCode, output},
                Map.of("output", output, "exitCode", exitCode));
    }

    public Integer getCode() {
        return (Integer) getDetails().getOrDefault("exitCode", null);
    }

    public String getOutput() {
        return (String) getDetails().getOrDefault("output", "");
    }
}
