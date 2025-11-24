package com.educode.educodeApi.enums;

import java.util.HashMap;
import java.util.Map;

public enum TaskType implements CodeEnum<Integer> {
    NON_INTERACTIVE(0),
    FULL_INTERACTIVE(1),
    OUTPUT_CHECKING(2);

    private final Integer code;
    private static final Map<Integer, TaskType> BY_CODE = new HashMap<>();

    static {
        for (TaskType t : values()) BY_CODE.put(t.code, t);
    }

    TaskType(Integer code) { this.code = code; }
    public Integer getCode() { return code; }

    public static TaskType fromCode(Integer code) {
        if (code == null) return null;
        TaskType t = BY_CODE.get(code);
        if (t == null) throw new IllegalArgumentException("Unknown TaskType code: " + code);
        return t;
    }
}
