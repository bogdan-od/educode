package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.*;
import com.educode.educodeApi.DTO.auth.LoginDTO;
import com.educode.educodeApi.DTO.auth.RefreshTokenDTO;
import com.educode.educodeApi.DTO.auth.SessionDTO;
import com.educode.educodeApi.DTO.user.UserCreateDTO;
import com.educode.educodeApi.DTO.user.UserDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.BadRequestError;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.functional.RequestContext;
import com.educode.educodeApi.lazyinit.RefreshTokenInclude;
import com.educode.educodeApi.lazyinit.UserInclude;
import com.educode.educodeApi.mappers.CheckerMapper;
import com.educode.educodeApi.mappers.DecisionMapper;
import com.educode.educodeApi.mappers.PuzzleMapper;
import com.educode.educodeApi.mappers.UserMapper;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.*;
import com.educode.educodeApi.security.MyUserDetails;
import com.educode.educodeApi.services.*;
import com.educode.educodeApi.utils.MappingUtils;
import com.educode.educodeApi.utils.PaginationUtils;
import com.educode.educodeApi.utils.StdOptional;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Контролер для управління користувачами та автентифікацією
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PuzzleRepository puzzleRepository;
    @Autowired
    private PuzzleDataRepository puzzleDataRepository;
    @Autowired
    private RequestContext requestContext;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private CheckerMapper checkerMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DecisionMapper decisionMapper;
    @Autowired
    private RoleService roleService;
    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private PuzzleMapper puzzleMapper;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Метод для реєстрації нового користувача
     * @param userDTO об'єкт користувача з даними для реєстрації
     * @return ResponseEntity з повідомленням про успіх або помилку реєстрації
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> reg(@RequestBody UserCreateDTO userDTO) {
        Map<String, String> response = new HashMap<>();
        short status = 200;

        log.info("Attempting to register new user: login='{}', email='{}'", userDTO.getLogin(), userDTO.getEmail());

        // Перевірка коректності даних користувача
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(userDTO);

        if (!violations.isEmpty()) {
            log.warn("Validation failed during registration: {}", violations);
            violations.forEach(violation -> response.put(violation.getPropertyPath().toString(), violation.getMessage()));
            status = 400;
        } else if (userRepository.findByLogin(userDTO.getLogin()).isPresent()) {
            log.warn("Registration attempt with an already existing login: {}", userDTO.getLogin());
            response.put("to", "login");
            response.put("error", "Користувач з таким login вже зареєстрован!");
            status = 401;
        } else if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            log.warn("Registration attempt with an already existing email: {}", userDTO.getEmail());
            response.put("to", "email");
            response.put("error", "Користувач з таким email вже зареєстрован!");
            status = 401;
        } else if (userDTO.getPassword().length() > 50) {
            log.warn("Attempt to register with a password longer than 50 characters");
            response.put("to", "password");
            response.put("error", "Пароль повинен містити максимум 50 символів");
            status = 401;
        } else {
            log.debug("Saving new user to the database: {}", userDTO.getLogin());

            // Шифрування пароля користувача
            userDTO.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            User user = userMapper.fromCreateDTO(userDTO);

            // Перевірка, чи існує роль "ROLE_USER" у базі даних
            user.addToRoles(roleService.findByType("USER"));

            userRepository.save(user);
            response.put("success", "Ви успішно зареєстровані!");
        }

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Метод для автентифікації користувача
     * @param loginRequest дані для входу (логін, пароль та інформація про пристрій)
     * @param request HTTP запит
     * @return ResponseEntity з токенами доступу або повідомленням про помилку
     */
    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginDTO loginRequest, HttpServletRequest request) {
        String login = loginRequest.getLogin();
        String password = loginRequest.getPassword();

        log.info("Login attempt for user: {}", login);

        Supplier<ResponseEntity<Map<String, String>>> responseEntitySupplier401 = () -> {
            Map<String, String> response = new HashMap<>();
            response.put("to", "password");
            response.put("error", "Невірні облікові дані!");
            return ResponseEntity.status(401).body(response);
        };

        String loginAttemptServiceKey = login + "#" + request.getRemoteAddr();

        if (loginAttemptService.isBlocked(loginAttemptServiceKey)) {
            Map<String, String> response = new HashMap<>();
            response.put("to", "password");
            response.put("error", "Занадто багато спроб входу, спробуйте пізніше");
            return ResponseEntity.status(401).body(response);
        }

        // Авторизація користувача
        try {
            MyUserDetails myUserDetails = (MyUserDetails) userDetailsService.loadUserByUsername(login);

            if (myUserDetails == null) {
                log.warn("User not found: {}", login);
                return responseEntitySupplier401.get();
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(myUserDetails, password, myUserDetails.getAuthorities())
            );

            log.debug("User '{}' successfully authenticated", login);

            User myUser = userRepository.findByLogin(myUserDetails.getUsername()).orElse(null);
            // Створення сесії користувача та токенів доступу та оновлення
            Session session = new Session(null, myUser, null, null, loginRequest.getDeviceName(), request.getHeader("X-Forwarded-For"), loginRequest.getDeviceType().toLowerCase(), null);
            session = sessionRepository.save(session);

            AccessToken accessToken = new AccessToken(null, myUser, session, UUID.randomUUID().toString(), null);
            RefreshToken refreshToken = new RefreshToken(null, myUser, session, UUID.randomUUID().toString(), null );

            accessTokenRepository.save(accessToken);
            refreshTokenRepository.save(refreshToken);

            // У відповідь додаємо JWT токен з інформацією про користувача та ролі
            String accessTokenString = jwtUtil.generateToken(login, myUser.getId(), accessToken.getToken(), myUser.getAllPermissions().stream().map(PermissionType::asString).collect(Collectors.toList()));

            requestContext.setAuthentication(authentication);

            log.info("User '{}' logged in successfully. Session ID: {}", login, session.getId());

            loginAttemptService.loginSucceeded(loginAttemptServiceKey);

            Map<String, String> response = new HashMap<>();
            response.put("success", "Ви успішно авторизувалися!");
            response.put("access_token", accessTokenString);
            response.put("refresh_token", refreshToken.getToken());
            return ResponseEntity.ok().body(response);
        } catch (AuthenticationException | NullPointerException e) {
            if (e instanceof AuthenticationException) {
                loginAttemptService.loginFailed(loginAttemptServiceKey);
            }

            log.warn("Authentication failed for user '{}': {}", login, e.getMessage());
            return responseEntitySupplier401.get();
        } catch (Exception e) {
            log.error("Unexpected error during login for user '{}': {}", login, e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("to", "password");
            response.put("error", "Помилка, не вдалось авторизуватися");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Метод для оновлення токену доступу
     * @param refreshTokenDTO дані для оновлення токену
     * @param request HTTP запит
     * @return ResponseEntity з новими токенами або повідомленням про помилку
     */
    @PostMapping("/auth/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO, HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        log.info("Refresh token request received");

        if (refreshTokenDTO.getAccessToken() == null || refreshTokenDTO.getRefreshToken() == null) {
            log.warn("Missing tokens in refresh request");
            response.put("error", "Токени не знайдено!");
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            return ResponseEntity.status(401).body(response);
        }

        String accessTokenString;

        try {
            // Видобуваємо токен доступу з JWT токена
            accessTokenString = jwtUtil.extractAccessToken(refreshTokenDTO.getAccessToken(), true);
            log.debug("Extracted access token string");
        } catch (MalformedJwtException | IllegalArgumentException e) {
            log.warn("Failed to extract access token: {}", e.getMessage());
            response.put("error", "Токени не знайдено!");
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            return ResponseEntity.status(401).body(response);
        }
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenDTO.getRefreshToken(), Set.of(RefreshTokenInclude.SESSION)).orElse(null);
        AccessToken accessToken = accessTokenRepository.findByToken(accessTokenString).orElse(null);

        if (accessToken != null && refreshToken != null && refreshToken.getValidUntil().isAfter(LocalDateTime.now())) {
            log.debug("Valid tokens found. Rotating tokens for user '{}'", accessToken.getUser().getLogin());

            accessToken.setToken(UUID.randomUUID().toString());
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.getSession().setDeviceName(refreshTokenDTO.getDeviceName());
            refreshToken.getSession().setIpAddress(request.getHeader("X-Forwarded-For"));

            refreshToken.getSession().setAccessToken(accessToken);
            refreshToken.getSession().setRefreshToken(refreshToken);

            accessTokenRepository.saveAndFlush(accessToken);
            refreshTokenRepository.saveAndFlush(refreshToken);
            sessionRepository.saveAndFlush(refreshToken.getSession());

            String newAccessTokenString = jwtUtil.generateToken(accessToken.getUser().getLogin(), accessToken.getUser().getId(), accessToken.getToken(), accessToken.getUser().getAllPermissions().stream().map(PermissionType::asString).collect(Collectors.toList()));
            response.put("access_token", newAccessTokenString);
            response.put("refresh_token", refreshToken.getToken());

            // Оновлюємо глобальну змінну tokenExpired на false, щоб у AuthRequestFilter не виконувалося перевірку на прострочення токену
            requestContext.setTokenExpired(false);
            log.info("Tokens successfully rotated for session '{}'", refreshToken.getSession().getId());
            return ResponseEntity.ok().body(response);
        } else {
            log.warn("Invalid or expired tokens");
            response.put("error", "Токени не знайдено!");
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * Метод для отримання інформації про користувача
     * @param login логін користувача
     * @return ResponseEntity з даними користувача або повідомленням про помилку
     */
    @GetMapping("/get/{login}")
    public ResponseEntity<UserDTO> getUser(@PathVariable(name = "login") String login) {
        log.info("Fetching user profile for login='{}'", login);

        User user = userService.findByLogin(login, Set.of(UserInclude.SESSIONS, UserInclude.DECISIONS, UserInclude.PUZZLES, UserInclude.PUZZLE_PUZZLEDATA)).orElse(null);

        if (user == null) {
            log.warn("User not found: '{}'", login);
            return ResponseEntity.status(404).build();
        }

        List<SessionDTO> sessionDTOList = new ArrayList<>();
        User loggedUser = userService.getAuthUser();
        String accessTokenString = userService.getAccessToken();

        log.debug("Authenticated user is '{}'; target user is '{}'", loggedUser != null ? loggedUser.getLogin() : "anonymous", user.getLogin());

        if (loggedUser != null && Objects.equals(loggedUser.getId(), user.getId())) {
            sessionDTOList = user.getSessions().stream().sorted(Comparator.comparing(Session::getCreatedAt).reversed()).map(session -> new SessionDTO(session, accessTokenString)).collect(Collectors.toList());
        }

        Map<Long, Decision> bestDecisionsMap = new HashMap<>();

        for (Decision decision : user.getDecisions()) {
            Long puzzleId = decision.getPuzzle().getId();
            bestDecisionsMap.compute(puzzleId, (id, existingDecision) ->
                existingDecision == null || decision.getScore() > existingDecision.getScore() ? decision : existingDecision
            );
        }

        List<Decision> userDecisions = new ArrayList<>(bestDecisionsMap.values()).stream().limit(20).toList();

        log.info("Returning profile for user '{}'", login);

        return ResponseEntity.ok(userMapper.toMaxViewDTO(
            user,
            StdOptional.of(sessionDTOList),
            StdOptional.of(MappingUtils.toDtoList(userDecisions, d -> decisionMapper.toDTO(d, StdOptional.ofNull(), StdOptional.empty(), false), DecisionDTO::getCreatedAt)),
            StdOptional.ofEmptyList())
        );
    }

    @GetMapping("/get/{login}/puzzles")
    public ResponseEntity<Map<String, Object>> getUserPuzzles(@PathVariable String login,
                                                              @RequestParam(defaultValue = "1") Integer page,
                                                              @RequestParam(defaultValue = "20") Integer limit) {
        page = PaginationUtils.validatePage(page);
        User user = userService.findByLogin(login, Set.of()).orElse(null);
        if (user == null)
            throw new NotFoundError("Не знайдено користувача");

        User authUser = userService.getAuthUserElseThrow();

        Page<Puzzle> puzzles;

        if (authUser == null || !Objects.equals(authUser.getId(), user.getId())) {
            puzzles = puzzleRepository.findAllByUserAndVisibleTrue(user, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id")));
        } else {
            puzzles = puzzleRepository.findAllByUser(user, PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id")));
        }

        return ResponseEntity.ok(Map.of(
            "content", puzzles.map(puzzle -> puzzleMapper.toMinDTO(puzzle)).toList(),
            "hasNext", puzzles.hasNext()
        ));
    }

    /**
     * Метод для видалення сесії користувача
     * @param id ідентифікатор сесії
     * @return ResponseEntity з повідомленням про успіх або помилку
     */
    @DeleteMapping("/session/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable(name = "id") Long id) {
        Map<String, String> response = new HashMap<>();

        log.info("Delete session request: sessionId={}", id);

        User loggedUser = userService.getAuthUser();

        if (loggedUser == null) {
            log.warn("Unauthorized delete session attempt for sessionId={}", id);
            return ResponseEntity.status(401).build();
        }

        Session session = sessionRepository.findById(id).orElse(null);

        if (session == null) {
            log.warn("Session not found: {}", id);
            response.put("error", "Сесія не знайдена!");
            return ResponseEntity.status(404).body(response);
        }

        if (!Objects.equals(loggedUser.getId(), session.getUser().getId())) {
            log.warn("User '{}' attempted to delete session '{}' owned by user '{}'", loggedUser.getLogin(), id, session.getUser().getLogin());
            return ResponseEntity.status(403).build();
        }

        if (Objects.equals(session.getAccessToken().getToken(), userService.getAccessToken())) {
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            log.debug("Indicated to client to delete local tokens for session '{}'", id);
        }

        sessionRepository.delete(session);
        log.info("Session '{}' deleted by user '{}'", id, loggedUser.getLogin());

        return ResponseEntity.ok(response);
    }

    /**
     * Метод для отримання таблиці лідерів
     * @return ResponseEntity зі списком користувачів, відсортованих за рейтингом
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserDTO>> getLeaderboard() {
        log.debug("Fetching top 100 users by rating");
        List<User> users = userRepository.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "rating"))).getContent();
        return ResponseEntity.ok(users.stream().map(userMapper::toMinViewDTO).collect(Collectors.toList()));
    }

    /**
     * Пошук користувачів за логіном
     * GET /api/users/search?q=...&page=1&limit=20
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<Map<String, String>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        User authUser = userService.getAuthUserElseThrow();
        page = PaginationUtils.validatePage(page);

        // Мінімальна довжина пошукового запиту (щоб уникнути надто широких результатів)
        if (q.length() < 3) {
            throw new BadRequestError("Пошуковий запит повинен містити щонайменше 3 символи");
        }

        // Виконуємо пошук
        Page<User> usersPage = userRepository.searchByLogin(
                q.toLowerCase(),
                PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "login"))
        );

        return ResponseEntity.ok(usersPage.getContent().stream().map(user -> Map.of(
                "id", user.getId().toString(),
                "title", user.getLogin(),
                "details", user.getName()
        )).toList());

        // Маппінг в DTO (без чутливих даних типу паролю)
//        Page<UserDTO> userSearchDTOs = usersPage.map(user -> userMapper.toMinViewDTO(user));
//
//        return ResponseEntity.ok(Map.of(
//                "users", userSearchDTOs.getContent(),
//                "total", usersPage.getTotalElements(),
//                "page", page + 1,
//                "limit", limit,
//                "hasNext", usersPage.hasNext(),
//                "query", q
//        ));
    }
}