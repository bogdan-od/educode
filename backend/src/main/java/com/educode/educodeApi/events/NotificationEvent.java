package com.educode.educodeApi.events;

import com.educode.educodeApi.enums.NotificationLevel;
import com.educode.educodeApi.models.User;
import org.springframework.context.ApplicationEvent;

public class NotificationEvent extends ApplicationEvent {
    private final String title;
    private final String message;
    private final User user;
    private final NotificationLevel level;

    public NotificationEvent(Object source, String title, String message, User user, NotificationLevel level) {
        super(source);
        this.title = title;
        this.message = message;
        this.user = user;
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public NotificationLevel getLevel() {
        return level;
    }
}
