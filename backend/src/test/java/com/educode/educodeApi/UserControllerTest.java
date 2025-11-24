package com.educode.educodeApi;

import com.educode.educodeApi.DTO.auth.LoginDTO;
import com.educode.educodeApi.DTO.auth.RefreshTokenDTO;
import com.educode.educodeApi.DTO.user.UserCreateDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.AccessTokenRepository;
import com.educode.educodeApi.repositories.RefreshTokenRepository;
import com.educode.educodeApi.repositories.SessionRepository;
import com.educode.educodeApi.services.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController
 * Tests cover registration, authentication, token management, user profiles, and search
 */
@DisplayName("UserController Integration Tests")
class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Nested
    @DisplayName("POST /api/user/register - User Registration")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should successfully register a new user with valid data")
        void shouldRegisterNewUser() throws Exception {
            // Given
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("newuser");
            userCreateDTO.setEmail("newuser@test.com");
            userCreateDTO.setPassword("SecurePassword123");
            userCreateDTO.setName("New User");

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("Ви успішно зареєстровані!"));

            // Verify user was saved in database
            User savedUser = userRepository.findByLogin("newuser").orElse(null);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo("newuser@test.com");
            assertThat(savedUser.getName()).isEqualTo("New User");
            assertThat(savedUser.getRoles()).isNotEmpty();
            assertThat(passwordEncoder.matches("SecurePassword123", savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should reject registration with duplicate login")
        void shouldRejectDuplicateLogin() throws Exception {
            // Given
            User existingUser = setupUser("existinguser");

            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("existinguser");
            userCreateDTO.setEmail("newemail@test.com");
            userCreateDTO.setPassword("Password123");
            userCreateDTO.setName("Test User");

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.to").value("login"))
                    .andExpect(jsonPath("$.error").value("Користувач з таким login вже зареєстрован!"));
        }

        @Test
        @DisplayName("Should reject registration with duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            // Given
            User existingUser = setupUser("existinguser");

            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("newlogin");
            userCreateDTO.setEmail("existinguser@test.com");
            userCreateDTO.setPassword("Password123");
            userCreateDTO.setName("Test User");

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.to").value("email"))
                    .andExpect(jsonPath("$.error").value("Користувач з таким email вже зареєстрован!"));
        }

        @Test
        @DisplayName("Should reject registration with password longer than 50 characters")
        void shouldRejectLongPassword() throws Exception {
            // Given
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("newuser");
            userCreateDTO.setEmail("newuser@test.com");
            userCreateDTO.setPassword("A".repeat(51)); // 51 characters
            userCreateDTO.setName("Test User");

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.to").value("password"))
                    .andExpect(jsonPath("$.error").value("Пароль повинен містити максимум 50 символів"));
        }

        @Test
        @DisplayName("Should reject registration with invalid email format")
        void shouldRejectInvalidEmail() throws Exception {
            // Given
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("newuser");
            userCreateDTO.setEmail("invalid-email"); // Invalid format
            userCreateDTO.setPassword("Password123");
            userCreateDTO.setName("Test User");

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.email").exists());
        }

        @Test
        @DisplayName("Should reject registration with missing required fields")
        void shouldRejectMissingFields() throws Exception {
            // Given
            UserCreateDTO userCreateDTO = new UserCreateDTO();
            userCreateDTO.setLogin("newuser");
            // Missing email, password, and name

            // When & Then
            mvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(userCreateDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/user/auth - User Authentication")
    class UserAuthenticationTests {

        @Test
        @DisplayName("Should successfully authenticate with valid credentials")
        void shouldAuthenticateWithValidCredentials() throws Exception {
            // Given
            User user = setupUser("testuser");
            user.setPassword(passwordEncoder.encode("correctPassword"));
            userRepository.save(user);

            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setLogin("testuser");
            loginDTO.setPassword("correctPassword");
            loginDTO.setDeviceName("Test Device");
            loginDTO.setDeviceType("desktop");

            // When & Then
            MvcResult result = mvc.perform(post("/api/user/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value("Ви успішно авторизувалися!"))
                    .andExpect(jsonPath("$.access_token").exists())
                    .andExpect(jsonPath("$.refresh_token").exists())
                    .andReturn();

            // Verify session was created
            assertThat(sessionRepository.findAll()).isNotEmpty();
            assertThat(accessTokenRepository.findAll()).isNotEmpty();
            assertThat(refreshTokenRepository.findAll()).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject authentication with incorrect password")
        void shouldRejectIncorrectPassword() throws Exception {
            // Given
            User user = setupUser("testuser");
            user.setPassword(passwordEncoder.encode("correctPassword"));
            userRepository.save(user);

            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setLogin("testuser");
            loginDTO.setPassword("wrongPassword");
            loginDTO.setDeviceName("Test Device");
            loginDTO.setDeviceType("desktop");

            // When & Then
            mvc.perform(post("/api/user/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.to").value("password"))
                    .andExpect(jsonPath("$.error").value("Невірні облікові дані!"));
        }

        @Test
        @DisplayName("Should reject authentication for non-existent user")
        void shouldRejectNonExistentUser() throws Exception {
            // Given
            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setLogin("nonexistent");
            loginDTO.setPassword("password");
            loginDTO.setDeviceName("Test Device");
            loginDTO.setDeviceType("desktop");

            // When & Then
            mvc.perform(post("/api/user/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.to").value("password"))
                    .andExpect(jsonPath("$.error").value("Невірні облікові дані!"));
        }
    }

    @Nested
    @DisplayName("POST /api/user/auth/refresh-token - Token Refresh")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should successfully refresh valid tokens")
        void shouldRefreshValidTokens() throws Exception {
            // Given
            User user = setupUser("testuser");
            Session session = createSession(user);
            AccessToken oldAccessToken = createAccessToken(user, session);
            RefreshToken oldRefreshToken = createRefreshToken(user, session);

            RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
            refreshTokenDTO.setAccessToken(generateJwtWithAccessToken(oldAccessToken));
            refreshTokenDTO.setRefreshToken(oldRefreshToken.getToken());
            refreshTokenDTO.setDeviceName("Updated Device");

            String oldAccessTokenString = oldAccessToken.getToken();
            String oldRefreshTokenString = oldRefreshToken.getToken();

            // When & Then
            mvc.perform(post("/api/user/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").exists())
                    .andExpect(jsonPath("$.refresh_token").exists())
                    .andExpect(jsonPath("$.access_token").value(not(equalTo(oldAccessTokenString))))
                    .andExpect(jsonPath("$.refresh_token").value(not(equalTo(oldRefreshTokenString))));

            // Verify tokens were rotated
            AccessToken newAccessToken = accessTokenRepository.findByToken(oldAccessTokenString).orElse(null);
            assertThat(newAccessToken).isNull(); // Old token should not exist anymore
        }

        @Test
        @DisplayName("Should reject refresh with expired refresh token")
        void shouldRejectExpiredRefreshToken() throws Exception {
            // Given
            User user = setupUser("testuser");
            Session session = createSession(user);
            AccessToken accessToken = createAccessToken(user, session);
            RefreshToken expiredRefreshToken = createRefreshToken(user, session, LocalDateTime.now().minusDays(1));

            RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
            refreshTokenDTO.setAccessToken(generateJwtWithAccessToken(accessToken));
            refreshTokenDTO.setRefreshToken(expiredRefreshToken.getToken());
            refreshTokenDTO.setDeviceName("Device");

            // When & Then
            mvc.perform(post("/api/user/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Токени не знайдено!"))
                    .andExpect(jsonPath("$.del_access_token").value("1"))
                    .andExpect(jsonPath("$.del_refresh_token").value("1"));
        }

        @Test
        @DisplayName("Should reject refresh with invalid access token")
        void shouldRejectInvalidAccessToken() throws Exception {
            // Given
            RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
            refreshTokenDTO.setAccessToken("invalid.jwt.token");
            refreshTokenDTO.setRefreshToken("someRefreshToken");
            refreshTokenDTO.setDeviceName("Device");

            // When & Then
            mvc.perform(post("/api/user/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Токени не знайдено!"))
                    .andExpect(jsonPath("$.del_access_token").value("1"))
                    .andExpect(jsonPath("$.del_refresh_token").value("1"));
        }

        @Test
        @DisplayName("Should reject refresh with missing tokens")
        void shouldRejectMissingTokens() throws Exception {
            // Given
            RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
            // Missing both tokens

            // When & Then
            mvc.perform(post("/api/user/auth/refresh-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Токени не знайдено!"))
                    .andExpect(jsonPath("$.del_access_token").value("1"))
                    .andExpect(jsonPath("$.del_refresh_token").value("1"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/get/{login} - Get User Profile")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should return user profile for existing user")
        void shouldReturnUserProfile() throws Exception {
            // Given
            User user = setupUser("testuser");
            user.setName("Test User Name");
            user.setRating(1500);
            userRepository.save(user);

            // When & Then
            mvc.perform(get("/api/user/get/testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value("testuser"))
                    .andExpect(jsonPath("$.name").value("Test User Name"))
                    .andExpect(jsonPath("$.email").value("testuser@test.com"))
                    .andExpect(jsonPath("$.rating").value(1500));
        }

        @Test
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistentUser() throws Exception {
            // When & Then
            mvc.perform(get("/api/user/get/nonexistent"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should include sessions when viewing own profile")
        void shouldIncludeSessionsForOwnProfile() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);
            Session session = createSession(user);
            AccessToken accessToken = createAccessToken(user, session);

            // Mock getAccessToken to return the current token
            when(userService.getAccessToken()).thenReturn(accessToken.getToken());

            // When & Then
            mvc.perform(get("/api/user/get/testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value("testuser"))
                    .andExpect(jsonPath("$.sessions").isArray())
                    .andExpect(jsonPath("$.sessions", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should not include sessions when viewing other user's profile")
        void shouldNotIncludeSessionsForOtherProfile() throws Exception {
            // Given
            User viewer = setupUser("viewer");
            User target = setupUser("target");
            loginUser(viewer);
            createSession(target);

            // When & Then
            mvc.perform(get("/api/user/get/target"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value("target"))
                    .andExpect(jsonPath("$.sessions").isEmpty());
        }

        @Test
        @DisplayName("Should include user's best decisions in profile")
        void shouldIncludeBestDecisions() throws Exception {
            // Given
            User user = setupUser("testuser");
            Puzzle puzzle = createPuzzle("Test Puzzle", user, true);

            // Create multiple decisions, service should return best one
            Decision decision1 = new Decision();
            decision1.setUser(user);
            decision1.setPuzzle(puzzle);
            decision1.setScore(50f);
            decision1.setCreatedAt(LocalDateTime.now());

            Decision decision2 = new Decision();
            decision2.setUser(user);
            decision2.setPuzzle(puzzle);
            decision2.setScore(100f); // Best score
            decision2.setCreatedAt(LocalDateTime.now());

            user.getDecisions().add(decision1);
            user.getDecisions().add(decision2);
            userRepository.saveAndFlush(user);

            // When & Then
            mvc.perform(get("/api/user/get/testuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value("testuser"))
                    .andExpect(jsonPath("$.decisions", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/user/session/{id} - Delete Session")
    class DeleteSessionTests {

        @Test
        @DisplayName("Should successfully delete own session")
        void shouldDeleteOwnSession() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);
            Session session = createSession(user);
            AccessToken accessToken = createAccessToken(user, session);

            // When & Then
            mvc.perform(delete("/api/user/session/" + session.getId()))
                    .andExpect(status().isOk());

            // Verify session was deleted
            assertThat(sessionRepository.findById(session.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should indicate token deletion when deleting current session")
        void shouldIndicateTokenDeletionForCurrentSession() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);
            Session session = createSession(user);
            AccessToken accessToken = createAccessToken(user, session);

            when(userService.getAccessToken()).thenReturn(accessToken.getToken());

            // When & Then
            mvc.perform(delete("/api/user/session/" + session.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.del_access_token").value("1"))
                    .andExpect(jsonPath("$.del_refresh_token").value("1"));
        }

        @Test
        @DisplayName("Should reject deleting session without authentication")
        void shouldRejectUnauthenticatedSessionDeletion() throws Exception {
            // Given
            User user = setupUser("testuser");
            Session session = createSession(user);
            loginUser(null); // Not authenticated

            // When & Then
            mvc.perform(delete("/api/user/session/" + session.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject deleting another user's session")
        void shouldRejectDeletingOtherUserSession() throws Exception {
            // Given
            User user1 = setupUser("user1");
            User user2 = setupUser("user2");
            loginUser(user1);
            Session user2Session = createSession(user2);

            // When & Then
            mvc.perform(delete("/api/user/session/" + user2Session.getId()))
                    .andExpect(status().isForbidden());

            // Verify session was not deleted
            assertThat(sessionRepository.findById(user2Session.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 for non-existent session")
        void shouldReturn404ForNonExistentSession() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            // When & Then
            mvc.perform(delete("/api/user/session/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Сесія не знайдена!"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/leaderboard - Get Leaderboard")
    class LeaderboardTests {

        @Test
        @DisplayName("Should return top users sorted by rating")
        void shouldReturnLeaderboardSortedByRating() throws Exception {
            // Given
            User user1 = setupUser("user1");
            user1.setName("First User");
            user1.setRating(1000);
            userRepository.save(user1);

            User user2 = setupUser("user2");
            user2.setName("Second User");
            user2.setRating(2000);
            userRepository.save(user2);

            User user3 = setupUser("user3");
            user3.setName("Third User");
            user3.setRating(1500);
            userRepository.save(user3);

            // When & Then
            mvc.perform(get("/api/user/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                    .andExpect(jsonPath("$[0].login").value("user2"))
                    .andExpect(jsonPath("$[0].rating").value(2000))
                    .andExpect(jsonPath("$[1].login").value("user3"))
                    .andExpect(jsonPath("$[1].rating").value(1500))
                    .andExpect(jsonPath("$[2].login").value("user1"))
                    .andExpect(jsonPath("$[2].rating").value(1000));
        }

        @Test
        @DisplayName("Should return empty leaderboard when no users exist")
        void shouldReturnEmptyLeaderboard() throws Exception {
            // Given
            userRepository.deleteAll();

            // When & Then
            mvc.perform(get("/api/user/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should limit leaderboard to 100 users")
        void shouldLimitLeaderboardTo100Users() throws Exception {
            // Given - create 150 users
            for (int i = 0; i < 150; i++) {
                User user = setupUser("user" + i);
                user.setRating(i * 10);
                userRepository.save(user);
            }

            // When & Then
            mvc.perform(get("/api/user/leaderboard"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(100)));
        }
    }

    @Nested
    @DisplayName("GET /api/user/autocomplete - Search Users")
    class UserSearchTests {

        @Test
        @DisplayName("Should search users by login with pagination")
        void shouldSearchUsersByLogin() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            User user1 = setupUser("alice");
            user1.setName("Alice Wonderland");
            userRepository.save(user1);

            User user2 = setupUser("alison");
            user2.setName("Alison Smith");
            userRepository.save(user2);

            User user3 = setupUser("bob");
            user3.setName("Bob Builder");
            userRepository.save(user3);

            // When & Then
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "ali")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].title").value(anyOf(equalTo("alice"), equalTo("alison"))))
                    .andExpect(jsonPath("$[1].title").value(anyOf(equalTo("alice"), equalTo("alison"))));
        }

        @Test
        @DisplayName("Should reject search with query shorter than 3 characters")
        void shouldRejectShortQuery() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            // When & Then
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "ab")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Пошуковий запит повинен містити щонайменше 3 символи"));
        }

        @Test
        @DisplayName("Should require authentication for search")
        void shouldRequireAuthenticationForSearch() throws Exception {
            // Given - no user logged in
            loginUser(null);

            // When & Then
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "test")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return empty results for non-matching query")
        void shouldReturnEmptyForNonMatchingQuery() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            User user1 = setupUser("alice");
            userRepository.save(user1);

            // When & Then
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "xyz")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should handle pagination correctly")
        void shouldHandlePaginationCorrectly() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            for (int i = 0; i < 25; i++) {
                User u = setupUser("user_s" + i);
                userRepository.save(u);
            }

            // When & Then - First page
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "user_s")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(10)));

            // When & Then - Second page
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "user_s")
                            .param("page", "2")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(10)));

            // When & Then - Third page
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "user_s")
                            .param("page", "3")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Should return user details in search results")
        void shouldReturnUserDetailsInResults() throws Exception {
            // Given
            User user = setupUser("testuser");
            loginUser(user);

            User searchUser = setupUser("searchme");
            searchUser.setName("Search Me User");
            userRepository.save(searchUser);

            // When & Then
            mvc.perform(get("/api/user/autocomplete")
                            .param("q", "search")
                            .param("page", "1")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(searchUser.getId().toString()))
                    .andExpect(jsonPath("$[0].title").value("searchme"))
                    .andExpect(jsonPath("$[0].details").value("Search Me User"));
        }
    }

    // Helper methods

    private Session createSession(User user) {
        Session session = new Session();
        session.setUser(user);
        session.setDeviceName("Test Device");
        session.setIpAddress("127.0.0.1");
        session.setDeviceType("desktop");
        session.setCreatedAt(LocalDateTime.now());
        user.getSessions().add(session);
        return sessionRepository.save(session);
    }

    private AccessToken createAccessToken(User user, Session session) {
        AccessToken accessToken = new AccessToken();
        accessToken.setUser(user);
        accessToken.setSession(session);
        session.setAccessToken(accessToken);
        accessToken.setToken(UUID.randomUUID().toString());
        user.getAccessTokens().add(accessToken);
        return accessTokenRepository.save(accessToken);
    }

    private RefreshToken createRefreshToken(User user, Session session, LocalDateTime validUntil) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setSession(session);
        if (validUntil != null) refreshToken.setValidUntil(validUntil);
        session.setRefreshToken(refreshToken);
        refreshToken.setToken(UUID.randomUUID().toString());
        user.getRefreshTokens().add(refreshToken);
        return refreshTokenRepository.save(refreshToken);
    }

    private RefreshToken createRefreshToken(User user, Session session) {
        return createRefreshToken(user, session, null);
    }

    private String generateJwtWithAccessToken(AccessToken accessToken) {
        return jwtUtil.generateToken(accessToken.getUser().getLogin(), accessToken.getUser().getId(), accessToken.getToken(), accessToken.getUser().getAllPermissions().stream().map(PermissionType::asString).collect(Collectors.toList()));
    }
}
