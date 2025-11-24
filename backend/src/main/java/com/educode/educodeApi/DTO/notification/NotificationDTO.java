package com.educode.educodeApi.DTO.notification;

import com.educode.educodeApi.enums.NotificationLevel;

import java.time.LocalDateTime;

public record NotificationDTO(String title, String content, NotificationLevel level, LocalDateTime date) {
}
