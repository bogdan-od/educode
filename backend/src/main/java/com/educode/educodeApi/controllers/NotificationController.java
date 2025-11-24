package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.notification.AllNotificationsDTO;
import com.educode.educodeApi.DTO.notification.NotificationDTO;
import com.educode.educodeApi.DTO.notification.NotificationPullDTO;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.UnauthorizedError;
import com.educode.educodeApi.mappers.NotificationMapper;
import com.educode.educodeApi.models.Notification;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.services.NotificationService;
import com.educode.educodeApi.services.UserService;
import com.educode.educodeApi.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    public NotificationController(UserService userService, NotificationService notificationService, NotificationMapper notificationMapper) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/pull")
    public ResponseEntity<NotificationPullDTO> pullCountNotifications() {
        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизовані");
        }

        return ResponseEntity.ok(notificationService.pullUnseenCount(authUser));
    }

    @GetMapping("/get")
    public ResponseEntity<AllNotificationsDTO> getAll(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);

        User authUser = userService.getAuthUser();

        if (authUser == null) {
            throw new UnauthorizedError("Ви не авторизовані");
        }

        if (limit > 1000) {
            throw new BadRequestError("На одній сторінці не може бути більш ніж 1000 повідомлень");
        }

        Page<Notification> notificationsPage = notificationService.findByUserAndMarkSeen(PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt")), authUser);
        Page<NotificationDTO> dtoPage = notificationsPage.map(notificationMapper::toDTO);

        return ResponseEntity.ok(new AllNotificationsDTO(dtoPage.getContent(), dtoPage.hasNext()));
    }
}
