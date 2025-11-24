package com.educode.educodeApi;

import com.educode.educodeApi.enums.TreeNodeType;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.*;
import com.educode.educodeApi.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * Абстрактный базовый класс для всех интеграционных тестов API.
 * Настраивает MockMvc, Spring Boot Test окружение и откатывает транзакции.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional // Откатывает транзакции после каждого теста
@ActiveProfiles("test") // Используем application-test.properties (предполагается H2)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @SpyBean
    protected UserService userService;

    // Подключаем репозитории для создания тестовых данных
    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected RoleService roleService; // Нужен для инициализации ролей

    @Autowired
    protected TreeNodeRepository treeNodeRepository;

    @Autowired
    protected NodeRepository nodeRepository;

    @Autowired
    protected GroupRepository groupRepository;

    @Autowired
    protected PuzzleRepository puzzleRepository;

    @Autowired
    protected CheckerRepository checkerRepository; // Предполагается, что он есть

    @Autowired
    protected TreeNodeMemberRepository memberRepository;

    @Autowired
    protected TreeNodeMemberService memberService;

    @Autowired
    protected HomeworkRepository homeworkRepository;

    @Autowired
    protected TreeNodeService treeNodeService;

    @Autowired
    protected TreeNodeResourceService resourceService;

    private boolean rolesInitialized = false;

    @BeforeEach
    public void setup() {
        // Инициализируем роли один раз
        if (!rolesInitialized) {
            roleService.initializeDefaultRoles();
            rolesInitialized = true;
        }

        // По умолчанию, пользователь не аутентифицирован
        when(userService.getAuthUser()).thenReturn(null);
        when(userService.getAuthUser(anySet())).thenReturn(null);
        doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow();
        doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow(anySet());
        doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow(anySet(), anySet());
    }

    /**
     * Вспомогательный метод для "логина" пользователя с заданными глобальными правами.
     * ИСПРАВЛЕНО: Этот метод теперь *только* создает/находит пользователя и мокирует findById.
     * Он *НЕ* логинит пользователя.
     */
    protected User setupUser(String login, com.educode.educodeApi.enums.PermissionType... globalPermissions) {
        User user = userRepository.findByLogin(login).orElseGet(() -> {
            User newUser = new User();
            newUser.setLogin(login);
            newUser.setEmail(login + "@test.com");
            newUser.setPassword("password"); // В тестах не важен
            newUser.setName("Test User " + login);
            return userRepository.save(newUser);
        });

        Set<Role> globalRoles = new HashSet<>();
        if (globalPermissions.length > 0) {
            // Создаем временную "тестовую" роль с нужными правами
            Role testRole = new Role(
                    "TEST_ROLE_" + login.toUpperCase(),
                    "Test role",
                    Stream.of(globalPermissions).collect(Collectors.toSet()),
                    999L,
                    com.educode.educodeApi.enums.RoleScope.GLOBAL
            );
            globalRoles.add(roleRepository.save(testRole));
        }

        // Добавляем базовую роль USER
        roleRepository.findByName("USER").ifPresent(globalRoles::add);
        user.setRoles(globalRoles);
        user = userRepository.save(user);

        // --- КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ ---
        // Мокируем findById, чтобы сервисы (контроллеры) могли находить этого пользователя
        User finalUser = user;
        when(userService.findById(user.getId())).thenReturn(finalUser);
        // -----------------------------

        return user;
    }

    /**
     * НОВЫЙ МЕТОД: Логинит пользователя (мокирует getAuthUser)
     */
    protected void loginUser(User user) {
        if (user == null) {
            when(userService.getAuthUser()).thenReturn(null);
            when(userService.getAuthUser(anySet())).thenReturn(null);
            doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow();
            doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow(anySet());
            doThrow(new com.educode.educodeApi.exceptions.UnauthorizedError("Вы не авторизованы")).when(userService).getAuthUserElseThrow(anySet(), anySet());
        } else {
            when(userService.getAuthUser()).thenReturn(user);
            doReturn(user).when(userService).getAuthUserElseThrow();
            doReturn(user).when(userService).getAuthUser(anySet());
            doReturn(user).when(userService).getAuthUserElseThrow(anySet());
            doReturn(user).when(userService).getAuthUserElseThrow(anySet(), anySet());
        }
    }


    /**
     * Вспомогательный метод для создания участника узла.
     */
    protected TreeNodeMember setupMember(TreeNode treeNode, User user, String... roleNames) {
        Set<Role> roles = Stream.of(roleNames)
                .map(roleService::findByType)
                .collect(Collectors.toSet());

        if (roles.isEmpty() && roleNames.length > 0) {
            // Если роли были указаны, но не найдены
            throw new IllegalArgumentException("Не удалось найти роли: " + String.join(",", roleNames));
        } else if (roles.isEmpty()) {
            // Если роли не указаны, добавляем базовую роль "MEMBER"
            Role memberRole = roleService.findByType("MEMBER");
            if (memberRole != null) {
                roles.add(memberRole);
            } else {
                // Фоллбэк, если даже "MEMBER" нет (хотя он должен быть)
                throw new IllegalStateException("Базовая роль 'MEMBER' не найдена. Убедитесь, что RoleService инициализирован.");
            }
        }

        TreeNodeMember member = new TreeNodeMember(user, treeNode, roles);
        return memberRepository.save(member);
    }

    /**
     * Создает узел (Node)
     */
    protected Node createNode(String title, TreeNode parent) {
        TreeNode treeNode = treeNodeService.createTreeNode(parent, TreeNodeType.NODE, true);
        Node node = new Node(title, "Description for " + title, treeNode);
        return nodeRepository.save(node);
    }

    /**
     * Создает группу (Group)
     */
    protected Group createGroup(String title, TreeNode parent) {
        TreeNode treeNode = treeNodeService.createTreeNode(parent, TreeNodeType.GROUP, false); // Группы не могут иметь детей
        Group group = new Group(title, "Description for " + title, treeNode);
        return groupRepository.save(group);
    }

    /**
     * Создает задачу (Puzzle)
     */
    protected Puzzle createPuzzle(String title, User author, boolean isVisible) {
        Puzzle puzzle = new Puzzle();
        puzzle.setTitle(title);
        puzzle.setDescription("Description");
        puzzle.setContent("Content longer than 20 chars"); // Исправлено
        puzzle.setUser(author);
        puzzle.setTimeLimit(1.0f);
        puzzle.setScore(0f);
        puzzle.setVisible(isVisible);
        puzzle.setEnabled(true);
        puzzle.setTaskType(com.educode.educodeApi.enums.TaskType.NON_INTERACTIVE);
        // Предполагаем наличие checker по умолчанию
        puzzle.setChecker(getTestChecker(author)); // Исправлено
        return puzzleRepository.save(puzzle);
    }

    /**
     * Создает или получает тестовый Checker
     * ИСПРАВЛЕНО: Всегда создает новый Checker для указанного автора,
     * чтобы избежать конфликта "Checker належить іншому користувачеві".
     */
    protected Checker getTestChecker(User author) {
        // Не ищем findById(1L)
        Checker checker = new Checker();
        checker.setUser(author);
        checker.setName("Test Checker for " + author.getLogin());
        checker.setSizeBytes(200);
        checker.setFilename("checker.cpp");
        checker.setLanguageId("python:3.11");
        return checkerRepository.save(checker);
    }
}
