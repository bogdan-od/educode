package com.educode.educodeApi.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "docker")
@Validated
public class DockerProperties {
    @NotNull(message = "Host must not be null")
    private String host;

    @NotNull(message = "Seccomp profile must not be null")
    private String seccompProfile;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getSeccompProfile() {
        return seccompProfile;
    }

    public void setSeccompProfile(String seccompProfile) {
        this.seccompProfile = seccompProfile;
    }
}
