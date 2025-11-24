package com.educode.educodeApi.enums;

public enum ContainerType {
    USER("run_user"), CHECKER("run_checker");

    private final String scriptRunType;

    ContainerType(String scriptRunType) {
        this.scriptRunType = scriptRunType;
    }

    public String getScriptRunType() {
        return scriptRunType;
    }
}
