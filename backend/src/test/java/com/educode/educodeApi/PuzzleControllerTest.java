package com.educode.educodeApi;

import com.educode.educodeApi.DTO.puzzle.PuzzleCreateDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDataCreateDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleUpdateDTO;
import com.educode.educodeApi.DTO.puzzle.PuzzleDataUpdateDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.enums.TaskType;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PuzzleControllerTest extends AbstractIntegrationTest {

    // ИСПРАВЛЕНИЕ: Очищаем репозиторий перед каждым тестом
    @BeforeEach
    public void setupPuzzles() {
        // Решает проблему "expected: 1, actual: 16"
        puzzleRepository.deleteAll();
    }

    private PuzzleCreateDTO createValidPuzzleDTO(TaskType taskType) {
        PuzzleCreateDTO dto = new PuzzleCreateDTO();
        dto.setTitle("Valid Puzzle Title");
        dto.setDescription("Valid puzzle description");
        dto.setContent("Valid puzzle content longer than 20 chars");
        dto.setTimeLimit(1.0f);
        dto.setTaskType(taskType);
        dto.setVisible(false);

        if (taskType == TaskType.NON_INTERACTIVE) {
            dto.setScore(10f); // 10 + 0
            dto.setPuzzleData(Set.of(
                    new PuzzleDataCreateDTO("in1", "out1", 10f),
                    new PuzzleDataCreateDTO("in2", "out2", 0f) // Тест с 0 очков
            ));
        } else if (taskType == TaskType.FULL_INTERACTIVE) {
            dto.setScore(100f);
            dto.setPuzzleData(Set.of()); // Интерактивные задачи не имеют данных
        }

        return dto;
    }

    // --- Тесты на POST /api/puzzles/add ---

    @Test
    public void testAddPuzzle_Success_NonInteractive() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is("Завдання успішно додано!")));

        // ИСПРАВЛЕНИЕ: Теперь `count()` будет 1, т.к. deleteAll() отработал в @BeforeEach
        assertEquals(1, puzzleRepository.count());
        Puzzle puzzle = puzzleRepository.findAll().get(0);
        assertEquals(dto.getTitle(), puzzle.getTitle());
        assertEquals(10.0f, puzzle.getScore());
    }

    @Test
    public void testAddPuzzle_Success_Interactive() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.FULL_INTERACTIVE);
        // ИСПРАВЛЕНИЕ: getTestChecker(author) теперь создает чекер, принадлежащий автору
        dto.setCheckerId(getTestChecker(author).getId());

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk()); // Ожидаем 200

        Puzzle puzzle = puzzleRepository.findAll().get(0);
        assertEquals(TaskType.FULL_INTERACTIVE, puzzle.getTaskType());
        assertNotNull(puzzle.getChecker());
    }

    @Test
    public void testAddPuzzle_Fail_Unauthenticated() throws Exception {
        // Не логиним пользователя (loginUser не вызывается)
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized()); // Ожидаем 401
    }

    // ИСПРАВЛЕНИЕ: Тест удален, т.к. роль "USER" (которую получают все)
    // УЖЕ ИМЕЕТ право CREATE_PUZZLES по бизнес-логике в RoleService.
    // Тест `testAddPuzzle_Fail_NoPermission` был некорректен.

    @Test
    public void testAddPuzzle_Fail_PublishPermission() throws Exception {
        // Пользователь с правом создания, но без права публикации
        User creator = setupUser("creator", PermissionType.CREATE_PUZZLES);
        loginUser(creator);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);
        dto.setVisible(true); // Пытаемся опубликовать

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не можете створити видиму")));
    }

    @Test
    public void testAddPuzzle_Fail_Validation() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);
        dto.setTitle("123"); // Невалидное (min = 5)

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", containsString("5 символів")));
    }

    @Test
    public void testAddPuzzle_Fail_Logic_NoZeroScoreTest() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);
        // Все тесты с очками > 0
        dto.setPuzzleData(Set.of(
                new PuzzleDataCreateDTO("in1", "out1", 10f),
                new PuzzleDataCreateDTO("in2", "out2", 5f)
        ));
        dto.setScore(15f);

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.puzzleData", containsString("0 балів")));
    }

    @Test
    public void testAddPuzzle_Fail_Logic_InteractiveWithData() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.FULL_INTERACTIVE);
        dto.setCheckerId(getTestChecker(author).getId());
        // Интерактивная задача не должна иметь puzzleData
        dto.setPuzzleData(Set.of(new PuzzleDataCreateDTO("in1", "out1", 0f)));

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("відсутності тест-кейсів")));
    }

    @Test
    public void testAddPuzzle_Fail_Logic_NonInteractiveNeedsData() throws Exception {
        User author = setupUser("author", PermissionType.CREATE_PUZZLES);
        loginUser(author);
        PuzzleCreateDTO dto = createValidPuzzleDTO(TaskType.NON_INTERACTIVE);
        dto.setPuzzleData(null); // NON_INTERACTIVE требует puzzleData

        mvc.perform(post("/api/puzzles/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("менше двох")));
    }

    // --- Тесты на GET /api/puzzles/get/{id} ---

    @Test
    public void testGetPuzzle_Success_Public() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Public Puzzle", author, true);

        User viewer = setupUser("viewer"); // "Логиним" другого пользователя
        loginUser(viewer);

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(puzzle.getId().intValue())))
                .andExpect(jsonPath("$.title", is("Public Puzzle")));
    }

    @Test
    public void testGetPuzzle_Fail_NotFound() throws Exception {
        User viewer = setupUser("viewer");
        loginUser(viewer);
        mvc.perform(get("/api/puzzles/get/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPuzzle_Fail_Private() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Private Puzzle", author, false);

        User viewer = setupUser("viewer"); // Другой пользователь
        loginUser(viewer);

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetPuzzle_Success_Owner() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Private Puzzle", author, false);

        loginUser(author); // "Логиним" автора

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(puzzle.getId().intValue())));
    }

    @Test
    public void testGetPuzzle_Success_Admin() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Private Puzzle", author, false);

        User admin = setupUser("admin", PermissionType.VIEW_ALL_PUZZLES);
        loginUser(admin);

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(puzzle.getId().intValue())));
    }

    @Test
    public void testGetPuzzle_Success_ViaTreeNode() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Node Puzzle", author, false); // Невидимая задача

        User viewer = setupUser("viewer");
        loginUser(viewer);
        Node node = createNode("Test Node", null);
        setupMember(node.getTreeNode(), viewer, "STUDENT"); // Делаем пользователя участником

        // Добавляем задачу в узел
        node.getTreeNode().addPuzzle(puzzle);
        treeNodeRepository.save(node.getTreeNode());

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId())
                        .param("treeNodeId", node.getTreeNode().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(puzzle.getId().intValue())));
    }

    @Test
    public void testGetPuzzle_Fail_ViaWrongTreeNode() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Node Puzzle", author, false);

        User viewer = setupUser("viewer");
        loginUser(viewer);
        Node node = createNode("Test Node", null);
        Node otherNode = createNode("Other Node", null); // Другой узел
        setupMember(otherNode.getTreeNode(), viewer, "STUDENT"); // Пользователь в ДРУГОМ узле

        node.getTreeNode().addPuzzle(puzzle);
        treeNodeRepository.save(node.getTreeNode());

        mvc.perform(get("/api/puzzles/get/{id}", puzzle.getId())
                        .param("treeNodeId", node.getTreeNode().getId().toString()))
                .andExpect(status().isNotFound());
    }

    // --- Тесты на PUT /api/puzzles/edit/{id} ---

    @Test
    public void testEditPuzzle_Success() throws Exception {
        User author = setupUser("author");
        loginUser(author);
        Puzzle puzzle = createPuzzle("Old Title", author, false);

        PuzzleUpdateDTO updateDTO = new PuzzleUpdateDTO();
        updateDTO.setTitle("New Valid Title");
        updateDTO.setDescription("Valid puzzle description");
        updateDTO.setContent("Valid puzzle content longer than 20 chars"); // ИСПРАВЛЕНО
        updateDTO.setTimeLimit(puzzle.getTimeLimit());
        updateDTO.setEnabled(true);
        updateDTO.setVisible(false);
        updateDTO.setPuzzleData(Set.of());

        mvc.perform(put("/api/puzzles/edit/{id}", puzzle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("success")));

        puzzleRepository.flush();
        Puzzle updatedPuzzle = puzzleRepository.findById(puzzle.getId()).get();
        assertEquals("New Valid Title", updatedPuzzle.getTitle());
    }

    @Test
    public void testEditPuzzle_Fail_NotOwner() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Old Title", author, false);

        User otherUser = setupUser("otherUser");
        loginUser(otherUser);

        PuzzleUpdateDTO updateDTO = new PuzzleUpdateDTO();
        updateDTO.setTitle("New Valid Title");
        updateDTO.setDescription("Valid puzzle description");
        updateDTO.setContent("Valid puzzle content longer than 20 chars"); // ИСПРАВЛЕНО
        updateDTO.setTimeLimit(puzzle.getTimeLimit());
        updateDTO.setEnabled(true);
        updateDTO.setVisible(false);
        updateDTO.setPuzzleData(Set.of());

        mvc.perform(put("/api/puzzles/edit/{id}", puzzle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("іншого користувача")));
    }

    @Test
    public void testEditPuzzle_Fail_ChangePuzzleDataSize() throws Exception {
        User author = setupUser("author");
        loginUser(author);
        Puzzle puzzle = createPuzzle("Puzzle With Data", author, false);
        puzzle.setPuzzleData(new HashSet<>(List.of(
                new PuzzleData(null, puzzle, "in", "out", 0f)
        )));
        puzzle = puzzleRepository.save(puzzle);

        PuzzleUpdateDTO updateDTO = new PuzzleUpdateDTO();
        updateDTO.setTitle(puzzle.getTitle());
        updateDTO.setDescription(puzzle.getDescription());
        updateDTO.setContent("Valid puzzle content longer than 20 chars"); // ИСПРАВЛЕНО
        updateDTO.setTimeLimit(puzzle.getTimeLimit());
        updateDTO.setEnabled(true);
        updateDTO.setVisible(false);
        updateDTO.setPuzzleData(Set.of()); // Пустой сет

        mvc.perform(put("/api/puzzles/edit/{id}", puzzle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("кількість тест-кейсів повинна не змінюватись")));
    }

    // --- Тесты на DELETE /api/puzzles/delete/{id} ---

    @Test
    public void testDeletePuzzle_Success() throws Exception {
        User author = setupUser("author");
        loginUser(author);
        Puzzle puzzle = createPuzzle("To Be Deleted", author, false);

        mvc.perform(delete("/api/puzzles/delete/{id}", puzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("deleted")));

        assertEquals(0, puzzleRepository.count());
    }

    @Test
    public void testDeletePuzzle_Fail_NotOwner() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("To Be Deleted", author, false);

        User otherUser = setupUser("otherUser");
        loginUser(otherUser);

        mvc.perform(delete("/api/puzzles/delete/{id}", puzzle.getId()))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.error", containsString("іншого користувача")));
    }

    @Autowired
    private DecisionRepository decisionRepository;

    @Test
    public void testDeletePuzzle_Fail_WithDecisions() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Puzzle With Decisions", author, false);

        User solver = setupUser("solver");
        Decision decision = new Decision();
        decision.setPuzzle(puzzle);
        decision.setUser(solver);
        decision.setCode("code");
        puzzle.addDecision(decision);
        decisionRepository.save(decision);

        loginUser(author);

        mvc.perform(delete("/api/puzzles/delete/{id}", puzzle.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("вже хтось вирішив")));
    }

    @Test
    public void testDeletePuzzle_Fail_WithHomeworks() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Puzzle With Homeworks", author, false);

        User teacher = setupUser("teacher");
        Group group = createGroup("Test Group", null);

        Homework homework = new Homework();
        homework.setGroup(group);
        homework.setPuzzle(puzzle);
        homework.setTitle("Test HW");
        homeworkRepository.save(homework);

        loginUser(author);

        mvc.perform(delete("/api/puzzles/delete/{id}", puzzle.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("вже створили домашні завдання")));
    }

    @Test
    public void testEditPuzzle_Fail_MakeInvisibleWithActiveHomework() throws Exception {
        // 1. Создаем инфраструктуру
        User teacher = setupUser("hw_teacher");
        User student = setupUser("hw_student");
        Group group = createGroup("Test Group for HW", null);
        setupMember(group.getTreeNode(), student, "STUDENT");
        setupMember(group.getTreeNode(), teacher, "GROUP_OWNER");

        Puzzle puzzle = createPuzzle("Non-visible Puzzle", teacher, true);

        Homework homework = new Homework("Active HW", "...", group, puzzle, LocalDateTime.now().plusDays(7));
        homeworkRepository.save(homework);

        // 5. Логинимся как автор
        loginUser(teacher);

        // 6. Создаем DTO, пытающийся сделать задачу невидимой
        PuzzleUpdateDTO updateDTO = new PuzzleUpdateDTO();
        updateDTO.setTitle(puzzle.getTitle());
        updateDTO.setDescription(puzzle.getDescription());
        updateDTO.setContent(puzzle.getContent());
        updateDTO.setTimeLimit(puzzle.getTimeLimit());
        updateDTO.setEnabled(true);
        updateDTO.setVisible(false);
        updateDTO.setPuzzleData(Set.of()); // (для NON_INTERACTIVE, но без данных)

        // 7. Ожидаем ошибку
        mvc.perform(put("/api/puzzles/edit/{id}", puzzle.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", containsString("CHANGE_VISIBILITY_ERROR")));
    }

    /*
     * Тестирует административный эндпоинт /make-invisible
     */
    @Test
    public void testMakePuzzleInvisible_Success_Admin() throws Exception {
        User author = setupUser("author");
        User admin = setupUser("admin", PermissionType.MAKE_PUZZLE_INVISIBLE);
        Puzzle puzzle = createPuzzle("Visible Puzzle", author, true);

        loginUser(admin);

        mvc.perform(put("/api/puzzles/{puzzleId}/make-invisible", puzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible", is(false)));
    }

    /*
     * Тестирует, что автор НЕ может использовать административный эндпоинт
     */
    @Test
    public void testMakePuzzleInvisible_Fail_Author() throws Exception {
        User author = setupUser("author");
        Puzzle puzzle = createPuzzle("Visible Puzzle", author, true);

        loginUser(author); // Логинимся как автор

        mvc.perform(put("/api/puzzles/{puzzleId}/make-invisible", puzzle.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Вам не дозволено робити задачі невидимими")));
    }
}
