package com.educode.educodeApi.enums;

public enum TreeNodeType implements CodeEnum<Integer> {
    FREE(0, "Вільний"),
    GROUP(1, "Група"),
    NODE(2, "Вузол");

    private final Integer code;
    private final String name;

    TreeNodeType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
