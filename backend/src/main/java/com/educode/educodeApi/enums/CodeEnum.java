package com.educode.educodeApi.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface CodeEnum<T> {
    T getCode();

    Map<Class<?>, Map<Object, Enum<?>>> CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    static <E extends Enum<E> & CodeEnum<T>, T> E fromCode(Class<E> enumClass, T code) {
        if (code == null) return null;

        // Берём (или создаём) кеш для данного enum
        Map<Object, Enum<?>> enumMap = CACHE.computeIfAbsent(enumClass, cls -> {
            Map<Object, Enum<?>> map = new HashMap<>();
            for (E constant : (E[]) cls.getEnumConstants()) {
                map.put(constant.getCode(), constant);
            }
            return map;
        });

        E result = (E) enumMap.get(code);
        if (result == null)
            throw new IllegalArgumentException("Unknown code " + code + " for enum " + enumClass.getSimpleName());
        return result;
    }
}
