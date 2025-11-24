package com.educode.educodeApi.enums;

public enum InvitationType implements CodeEnum<Integer> {
    PUBLIC(0, "Публічне"),
    LIMITED_LIST(1, "За списком користувачів"),
    SINGLE_USE(2, "Для одного користувача"),
    NODE_BASED(3, "Для учасників вузла");

    private final Integer code;
    private final String description;

    InvitationType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
