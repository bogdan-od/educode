package com.educode.educodeApi.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "container")
@Validated
public class ContainerProperties {
    @NotNull(message = "Memory limit must not be null")
    private Integer memoryLimit;
    @NotNull(message = "Time limit must not be null")
    private Float timeLimit;
    @NotNull(message = "Checker memory limit must not be null")
    private Integer checkerMemoryLimit;
    @NotNull(message = "Checker time addition must not be null")
    private Float checkerTimeAddition;

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Float getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Float timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getCheckerMemoryLimit() {
        return checkerMemoryLimit;
    }

    public void setCheckerMemoryLimit(Integer checkerMemoryLimit) {
        this.checkerMemoryLimit = checkerMemoryLimit;
    }

    public Float getCheckerTimeAddition() {
        return checkerTimeAddition;
    }

    public void setCheckerTimeAddition(Float checkerTimeAddition) {
        this.checkerTimeAddition = checkerTimeAddition;
    }

    public float getCheckerTimeLimit(float limit) {
        return limit + checkerTimeAddition;
    }
}
