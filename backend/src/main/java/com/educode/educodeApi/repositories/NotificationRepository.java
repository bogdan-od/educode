package com.educode.educodeApi.repositories;

import com.educode.educodeApi.enums.NotificationLevel;
import com.educode.educodeApi.models.Notification;
import com.educode.educodeApi.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Long countByUserAndSeenFalseAndNotificationLevel(User user, NotificationLevel notificationLevel);

    default Long countUnreadInfo(User user) {
        return countByUserAndSeenFalseAndNotificationLevel(user, NotificationLevel.INFO);
    }
    default Long countUnreadWarn(User user) {
        return countByUserAndSeenFalseAndNotificationLevel(user, NotificationLevel.WARN);
    }
    default Long countUnreadCritical(User user) {
        return countByUserAndSeenFalseAndNotificationLevel(user, NotificationLevel.CRITICAL);
    }

    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.seen = true WHERE n.id IN (:ids)")
    void markSeenByIds(@Param("ids") Collection<Long> ids);

    Page<Notification> findAllByUser(Pageable pageable, User user);
}
