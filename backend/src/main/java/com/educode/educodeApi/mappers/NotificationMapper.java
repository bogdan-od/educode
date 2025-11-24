package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.notification.NotificationDTO;
import com.educode.educodeApi.models.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public NotificationDTO toDTO(Notification notification) {
        return new NotificationDTO(notification.getTitle(), notification.getContent(), notification.getNotificationLevel(), notification.getCreatedAt());
    }
}
