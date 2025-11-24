package com.educode.educodeApi.enums.converters;

import com.educode.educodeApi.enums.NotificationLevel;

public class NotificationLevelConverter extends GenericCodeEnumConverter<NotificationLevel, Integer> {
    public NotificationLevelConverter() {
        super(NotificationLevel.class);
    }
}
