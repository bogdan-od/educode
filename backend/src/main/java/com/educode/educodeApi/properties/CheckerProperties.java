package com.educode.educodeApi.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "checker")
@Validated
public class CheckerProperties {
    @NotNull(message = "Checker base path must not be null")
    private String basePath;

    @NotNull(message = "Checker per user bytes limit must not be null")
    private Long bytesPerUserLimit;

    @NotNull(message = "Checker per user amount limit must not be null")
    private Long amountPerUserLimit;

    @NotNull(message = "Checker code file size limit must not be null")
    private Long codeFileSizeLimit;

    @NotNull(message = "Checker base log path must not be null")
    private String baseLogPath;

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Long getBytesPerUserLimit() {
        return bytesPerUserLimit;
    }

    public void setBytesPerUserLimit(Long bytesPerUserLimit) {
        this.bytesPerUserLimit = bytesPerUserLimit;
    }

    public Long getAmountPerUserLimit() {
        return amountPerUserLimit;
    }

    public void setAmountPerUserLimit(Long amountPerUserLimit) {
        this.amountPerUserLimit = amountPerUserLimit;
    }

    public Long getCodeFileSizeLimit() {
        return codeFileSizeLimit;
    }

    public void setCodeFileSizeLimit(Long codeFileSizeLimit) {
        this.codeFileSizeLimit = codeFileSizeLimit;
    }

    public String getBaseLogPath() {
        return baseLogPath;
    }

    public void setBaseLogPath(String baseLogPath) {
        this.baseLogPath = baseLogPath;
    }
}
