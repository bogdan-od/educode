package com.educode.educodeApi.enums;

import java.util.HashMap;
import java.util.Map;

public enum NotificationLevel implements CodeEnum<Integer> {
    INFO(0),
    WARN(1),
    CRITICAL(2);

    private final Integer code;
    private static final Map<Integer, NotificationLevel> BY_CODE = new HashMap<>();

    static {
        for (NotificationLevel t : values()) BY_CODE.put(t.code, t);
    }

    NotificationLevel(Integer code) { this.code = code; }
    public Integer getCode() { return code; }

    public static NotificationLevel fromCode(Integer code) {
        if (code == null) return null;
        NotificationLevel t = BY_CODE.get(code);
        if (t == null) throw new IllegalArgumentException("Unknown TaskType code: " + code);
        return t;
    }
}
