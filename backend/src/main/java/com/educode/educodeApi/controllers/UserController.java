package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.LoginDTO;
import com.educode.educodeApi.DTO.RefreshTokenDTO;
import com.educode.educodeApi.DTO.SessionDTO;
import com.educode.educodeApi.DTO.UserDTO;
import com.educode.educodeApi.functional.GlobalVariables;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.*;
import com.educode.educodeApi.security.MyUserDetails;
import com.educode.educodeApi.services.UserDetailsService;
import com.educode.educodeApi.services.UserService;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.educode.educodeApi.services.JwtUtil;

import java.time.LocalDateTime;
import java.util.*;
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

    /**
     * Метод для реєстрації нового користувача
     * @param user об'єкт користувача з даними для реєстрації
     * @return ResponseEntity з повідомленням про успіх або помилку реєстрації
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> reg(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        short status = 200;

        // Встановлення початкових значень для нового користувача, щоб запобігти випадкам, коли користувач спробує послати на сервер одне з цих полей з своїми неправильними значеннями
        user.setRating(0);
        user.setDecisions(new HashSet<>());
        user.setId(null);
        user.setAccessTokens(new HashSet<>());
        user.setRefreshTokens(new HashSet<>());
        user.setSessions(new HashSet<>());
        user.setRoles(new HashSet<>());
        user.setPuzzles(new HashSet<>());

        // Перевірка коректності даних користувача
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            violations.forEach(violation -> response.put(violation.getPropertyPath().toString(), violation.getMessage()));
            status = 400;
        } else if (userRepository.findByLogin(user.getLogin()).isPresent()) {
            response.put("to", "login");
            response.put("error", "Користувач з таким login вже зареєстрован!");
            status = 401;
        } else if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("to", "email");
            response.put("error", "Користувач з таким email вже зареєстрован!");
            status = 401;
        } else if (user.getPassword().length() > 50) {
            response.put("to", "password");
            response.put("error", "Пароль повинен містити максимум 50 символів");
            status = 401;
        } else {
            // Шифрування пароля користувача
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            // Перевірка, чи існує роль "ROLE_USER" у базі даних
            Optional<Role> role = roleRepository.findByName("ROLE_USER");
            if (role.isPresent())
                user.addToRoles(role.get());
            else {
                Role role1 = roleRepository.save(new Role("ROLE_USER"));
                user.addToRoles(role1);
            }

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

        // Авторизація користувача
        try {
            MyUserDetails myUserDetails = (MyUserDetails) userDetailsService.loadUserByUsername(login);

            if (myUserDetails == null) {
                Map<String, String> response = new HashMap<>();
                response.put("to", "password");
                response.put("error", "Невірні облікові дані!");
                return ResponseEntity.status(401).body(response);
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(myUserDetails, password, myUserDetails.getAuthorities())
            );

            User myUser = userRepository.findByLogin(myUserDetails.getUsername()).orElse(null);
            // Створення сесії користувача та токенів доступу та оновлення
            Session session = new Session(null, myUser, null, null, loginRequest.getDeviceName(), request.getHeader("X-Forwarded-For"), loginRequest.getDeviceType().toLowerCase(), null);
            session = sessionRepository.save(session);

            AccessToken accessToken = new AccessToken(null, myUser, session, UUID.randomUUID().toString(), null);
            RefreshToken refreshToken = new RefreshToken(null, myUser, session, UUID.randomUUID().toString(), null );

            accessTokenRepository.save(accessToken);
            refreshTokenRepository.save(refreshToken);

            // У відповідь замість токену доступу додаємо JWT токен з інформацією про користувача та ролі
            String accessTokenString = jwtUtil.generateToken(login, accessToken.getToken(), myUser.getRoles().stream().map(Role::getName).collect(Collectors.toList()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            Map<String, String> response = new HashMap<>();
            response.put("success", "Ви успішно авторизувалися!");
            response.put("access_token", accessTokenString);
            response.put("refresh_token", refreshToken.getToken());
            return ResponseEntity.ok().body(response);
        } catch (AuthenticationException | NullPointerException e) {
            Map<String, String> response = new HashMap<>();
            response.put("to", "password");
            response.put("error", "Невірні облікові дані!");
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
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

        if (refreshTokenDTO.getAccessToken() == null || refreshTokenDTO.getRefreshToken() == null) {
            response.put("error", "Токени не знайдено!");
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            return ResponseEntity.status(401).body(response);
        }

        String accessTokenString;

        try {
            // Видобуваємо токен доступу з JWT токена
            accessTokenString = jwtUtil.extractAccessToken(refreshTokenDTO.getAccessToken(), true);
        } catch (MalformedJwtException | IllegalArgumentException e) {
            response.put("error", "Токени не знайдено!");
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
            return ResponseEntity.status(401).body(response);
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenDTO.getRefreshToken()).orElse(null);
        AccessToken accessToken = accessTokenRepository.findByToken(accessTokenString).orElse(null);

        if (accessToken != null && refreshToken != null && refreshToken.getValidUntil().isAfter(LocalDateTime.now())) {
            accessToken.setToken(UUID.randomUUID().toString());
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.getSession().setDeviceName(refreshTokenDTO.getDeviceName());
            refreshToken.getSession().setIpAddress(request.getHeader("X-Forwarded-For"));

            accessTokenRepository.save(accessToken);
            refreshTokenRepository.save(refreshToken);
            sessionRepository.save(refreshToken.getSession());

            String newAccessTokenString = jwtUtil.generateToken(accessToken.getUser().getLogin(), accessToken.getToken(), accessToken.getUser().getRoles().stream().map(Role::getName).collect(Collectors.toList()));
            response.put("access_token", newAccessTokenString);
            response.put("refresh_token", refreshToken.getToken());

            // Оновлюємо глобальну змінну tokenExpired на false, щоб у AuthRequestFilter не виконувалося перевірку на прострочення токену
            GlobalVariables.tokenExpired = false;
            return ResponseEntity.ok().body(response);
        } else {
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
        User user = userRepository.findByLogin(login).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        List<SessionDTO> sessionDTOList = new ArrayList<>();
        User loggedUser = userService.getAuthUser();
        String accessTokenString = userService.getAccessToken();

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

        List<Decision> userDecisions = new ArrayList<>(bestDecisionsMap.values()).stream().limit(20).collect(Collectors.toList());

        return ResponseEntity.ok(new UserDTO(user.getName(), user.getLogin(), user.getEmail(), user.getRating(), sessionDTOList, userDecisions, user.getPuzzles()));
    }

    /**
     * Метод для видалення сесії користувача
     * @param id ідентифікатор сесії
     * @return ResponseEntity з повідомленням про успіх або помилку
     */
    @DeleteMapping("/session/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable(name = "id") Long id) {
        Map<String, String> response = new HashMap<>();

        User loggedUser = userService.getAuthUser();

        if (loggedUser == null) {
            return ResponseEntity.status(401).build();
        }

        Session session = sessionRepository.findById(id).orElse(null);

        if (session == null) {
            response.put("error", "Сесія не знайдена!");
            return ResponseEntity.status(404).body(response);
        }

        if (!Objects.equals(loggedUser.getId(), session.getUser().getId())) {
            return ResponseEntity.status(403).build();
        }

        sessionRepository.delete(session);

        if (Objects.equals(session.getAccessToken().getToken(), userService.getAccessToken())) {
            response.put("del_access_token", "1");
            response.put("del_refresh_token", "1");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Метод для отримання таблиці лідерів
     * @return ResponseEntity зі списком користувачів, відсортованих за рейтингом
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserDTO>> getLeaderboard() {
        List<User> users = userRepository.findAll(PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "rating"))).getContent();
        return ResponseEntity.ok(users.stream().map(user -> new UserDTO(user.getName(), user.getLogin(), user.getEmail(), user.getRating())).collect(Collectors.toList()));
    }
}