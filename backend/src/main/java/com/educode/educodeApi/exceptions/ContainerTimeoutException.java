package com.educode.educodeApi.exceptions;

import java.util.Map;

/**
 * Виняток, який виникає при перевищенні часу очікування контейнера
 * Використовується для обробки ситуацій, коли виконання коду в контейнері займає більше часу, ніж дозволено
 */
public class ContainerTimeoutException extends ContainerException {
    public ContainerTimeoutException(long time) {
        super("TIMEOUT",
                "Execution timed out after " + time + " milliseconds",
                "Timeout: {} milliseconds", new Object[]{time},
                Map.of("time", time));
    }

    public Long getTime() {
        return (Long) getDetails().getOrDefault("time", 0);
    }
}
