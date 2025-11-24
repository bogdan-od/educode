package com.educode.educodeApi;

import com.educode.educodeApi.enums.NotificationLevel;
import com.educode.educodeApi.models.Notification;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NotificationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private User testUser;

    @BeforeEach
    public void setupNotifications() {
        notificationRepository.deleteAll(); // Очищаем репо
        testUser = setupUser("notifyUser");
    }

    // --- Тесты для GET /api/notifications/pull ---

    @Test
    public void testPullCount_Unauthenticated() throws Exception {
        mvc.perform(get("/api/notifications/pull"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testPullCount_Success_Authenticated() throws Exception {
        loginUser(testUser);

        // Создаем уведомления
        notificationRepository.save(new Notification(testUser, "Info 1", "...", NotificationLevel.INFO));
        notificationRepository.save(new Notification(testUser, "Info 2", "...", NotificationLevel.INFO));
        notificationRepository.save(new Notification(testUser, "Warn 1", "...", NotificationLevel.WARN));
        notificationRepository.save(new Notification(testUser, "Crit 1", "...", NotificationLevel.CRITICAL));
        // Одно прочитанное
        Notification seen = new Notification(testUser, "Seen", "...", NotificationLevel.INFO);
        seen.setSeen(true);
        notificationRepository.save(seen);

        mvc.perform(get("/api/notifications/pull"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countInfo", is(2)))
                .andExpect(jsonPath("$.countWarn", is(1)))
                .andExpect(jsonPath("$.countCritical", is(1)));
    }

    @Test
    public void testPullCount_Success_NoNotifications() throws Exception {
        loginUser(testUser);

        mvc.perform(get("/api/notifications/pull"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countInfo", is(0)))
                .andExpect(jsonPath("$.countWarn", is(0)))
                .andExpect(jsonPath("$.countCritical", is(0)));
    }

    // --- Тесты для GET /api/notifications/get ---

    @Test
    public void testGetAll_Unauthenticated() throws Exception {
        mvc.perform(get("/api/notifications/get"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetAll_Success_MarksAsSeen() throws Exception {
        loginUser(testUser);

        Notification n1 = notificationRepository.save(new Notification(testUser, "Info 1", "...", NotificationLevel.INFO));
        Notification n2 = notificationRepository.save(new Notification(testUser, "Warn 1", "...", NotificationLevel.WARN));

        assertEquals(2, notificationRepository.countUnreadInfo(testUser) + notificationRepository.countUnreadWarn(testUser));

        mvc.perform(get("/api/notifications/get")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications", hasSize(2)))
                .andExpect(jsonPath("$.notifications[0].title", is(n2.getTitle()))) // Сортировка по DESC
                .andExpect(jsonPath("$.notifications[1].title", is(n1.getTitle())))
                .andExpect(jsonPath("$.hasNextPage", is(false)));

        // Главная проверка: уведомления должны стать прочитанными
        assertEquals(0, notificationRepository.countUnreadInfo(testUser) + notificationRepository.countUnreadWarn(testUser));
        assertTrue(notificationRepository.findById(n1.getId()).get().getSeen());
        assertTrue(notificationRepository.findById(n2.getId()).get().getSeen());
    }

    @Test
    public void testGetAll_Success_Pagination() throws Exception {
        loginUser(testUser);

        for (int i = 0; i < 25; i++) {
            notificationRepository.save(new Notification(testUser, "Notify " + i, "...", NotificationLevel.INFO));
        }

        assertEquals(25, notificationRepository.countUnreadInfo(testUser));

        // Запрашиваем первую страницу (limit=20 - по умолчанию)
        mvc.perform(get("/api/notifications/get")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications", hasSize(20)))
                .andExpect(jsonPath("$.hasNextPage", is(true)));

        // Проверяем, что первые 20 отмечены как прочитанные
        assertEquals(5, notificationRepository.countUnreadInfo(testUser));

        // Запрашиваем вторую страницу
        mvc.perform(get("/api/notifications/get")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications", hasSize(5)))
                .andExpect(jsonPath("$.hasNextPage", is(false)));

        // Проверяем, что все отмечены как прочитанные
        assertEquals(0, notificationRepository.countUnreadInfo(testUser));
    }

    @Test
    public void testGetAll_Fail_LimitTooHigh() throws Exception {
        loginUser(testUser);

        mvc.perform(get("/api/notifications/get")
                        .param("limit", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("1000 повідомлень")));
    }
}
