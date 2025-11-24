package com.educode.educodeApi.DTO.notification;

import java.util.List;

public record AllNotificationsDTO(List<NotificationDTO> notifications, Boolean hasNextPage) {
}
