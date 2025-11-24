package com.educode.educodeApi;

import com.educode.educodeApi.DTO.homework.HomeworkCreateDTO;
import com.educode.educodeApi.models.*;
import com.educode.educodeApi.repositories.DecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class HomeworkControllerTest extends AbstractIntegrationTest {

    private User teacher;
    private User student;
    private Group group;
    private Puzzle puzzle;

    @BeforeEach
    public void setupHomework() {
        // ИСПРАВЛЕНИЕ: Используем setupUser для создания/поиска И мокирования findById
        teacher = setupUser("teacher");
        student = setupUser("student");

        group = createGroup("Test Group", null);

        // Учитель - владелец группы, студент - участник
        setupMember(group.getTreeNode(), teacher, "GROUP_OWNER");
        setupMember(group.getTreeNode(), student, "STUDENT");

        puzzle = createPuzzle("HW Puzzle", teacher, false);
    }

    // --- Тесты на POST /api/homework/create ---

    @Test
    public void testCreateHomework_Success() throws Exception {
        loginUser(teacher);

        HomeworkCreateDTO dto = new HomeworkCreateDTO(
                "New Homework",
                "Do this homework",
                group.getId(),
                puzzle.getId(),
                LocalDateTime.now().plusDays(7)
        );

        mvc.perform(post("/api/homework/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()));

        assertEquals(1, homeworkRepository.count());
    }

    @Test
    public void testCreateHomework_Fail_NoPermission() throws Exception {
        loginUser(student);

        HomeworkCreateDTO dto = new HomeworkCreateDTO(
                "New Homework",
                "Do this homework",
                group.getId(),
                puzzle.getId(),
                LocalDateTime.now().plusDays(7)
        );

        mvc.perform(post("/api/homework/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden()) // 403
                .andExpect(jsonPath("$.error", containsString("не дозволено створювати домашні завдання")));
    }

    @Test
    public void testCreateHomework_Fail_GroupNotFound() throws Exception {
        loginUser(teacher);

        HomeworkCreateDTO dto = new HomeworkCreateDTO(
                "New Homework",
                "Do this homework",
                9999L, // Несуществующая группа
                puzzle.getId(),
                LocalDateTime.now().plusDays(7)
        );

        mvc.perform(post("/api/homework/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.error", containsString("Не знайдено групу")));
    }

    // --- Тесты на GET /api/homework/{id} ---

    @Test
    public void testViewHomework_Success_AsMember() throws Exception {
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));

        loginUser(student);

        mvc.perform(get("/api/homework/{id}", hw.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(hw.getId().intValue())))
                .andExpect(jsonPath("$.hasSubmitted", is(false)));
    }

    @Test
    public void testViewHomework_Fail_AsNonMember() throws Exception {
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));

        User otherUser = setupUser("otherUser"); // "Логиним" постороннего
        loginUser(otherUser);

        mvc.perform(get("/api/homework/{id}", hw.getId()))
                .andExpect(status().isForbidden()) // 403
                .andExpect(jsonPath("$.error", containsString("не дозволено переглядати")));
    }

    // --- Тесты на DELETE /api/homework/{id} ---

    @Test
    public void testDeleteHomework_Success() throws Exception {
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));

        loginUser(teacher);

        mvc.perform(delete("/api/homework/{id}", hw.getId()))
                .andExpect(status().isOk());

//        assertEquals(0, homeworkRepository.count());
        assertFalse(homeworkRepository.findById(hw.getId()).isPresent());
    }

    @Autowired
    private DecisionRepository decisionRepository;

    @Test
    public void testDeleteHomework_Fail_WithSubmissions() throws Exception {
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));

        Decision decision = new Decision();
        decision.setHomework(hw);
        decision.setUser(student); // student из @BeforeEach
        decision.setPuzzle(puzzle);
        decision.setCode("code");
        decisionRepository.save(decision);

        loginUser(teacher);

        mvc.perform(delete("/api/homework/{id}", hw.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("вже має здані рішення")));
    }

    // --- Тесты на GET /api/homework/group/{groupId} ---

    @Test
    public void testViewByGroup_Success_IncludeExpired() throws Exception {
        homeworkRepository.save(new Homework("Active HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));
        homeworkRepository.save(new Homework("Expired HW", "Desc", group, puzzle, LocalDateTime.now().minusDays(1)));

        loginUser(student);
        // ИСПРАВЛЕНИЕ: `setupMember` УЖЕ вызван в @BeforeEach, доп. вызов не нужен

        mvc.perform(get("/api/homework/group/{groupId}", group.getId())
                        .param("includeExpired", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeworks.content", hasSize(2)));
    }

    @Test
    public void testViewByGroup_Success_OnlyActive() throws Exception {
        homeworkRepository.save(new Homework("Active HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));
        homeworkRepository.save(new Homework("Expired HW", "Desc", group, puzzle, LocalDateTime.now().minusDays(1)));

        loginUser(student);
        // ИСПРАВЛЕНИЕ: `setupMember` УЖЕ вызван в @BeforeEach

        mvc.perform(get("/api/homework/group/{groupId}", group.getId())
                        .param("includeExpired", "false")) // По умолчанию
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeworks.content", hasSize(1)))
                .andExpect(jsonPath("$.homeworks.content[0].title", is("Active HW")));
    }

    @Test
    public void testGetSubmissions_Success_AsGroupOwner_NoCode() throws Exception {
        // @BeforeEach уже создал teacher (как GROUP_OWNER) и student
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));
        Decision decision = new Decision();
        decision.setHomework(hw);
        decision.setUser(student);
        decision.setPuzzle(puzzle);
        decision.setCode("student's secret code");
        decisionRepository.save(decision);

        loginUser(teacher); // teacher из @BeforeEach является GROUP_OWNER

        mvc.perform(get("/api/homework/{id}/submissions", hw.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(decision.getId().intValue())))
                .andExpect(jsonPath("$.content[0].code", is(nullValue()))); // Код должен быть скрыт
    }

    /*
     * Тестирует эндпоинт /submissions
     * Проверяет, что пользователь с ролью TEACHER (у которой есть VIEW_HOMEWORK_DECISIONS_CODE)
     * видит исходный код решений.
     */
    @Test
    public void testGetSubmissions_Success_AsTeacher_WithCode() throws Exception {
        // Создаем нового "настоящего" учителя
        User realTeacher = setupUser("real_teacher");
        // Даем ему роль TEACHER (вместо GROUP_OWNER)
        setupMember(group.getTreeNode(), realTeacher, "TEACHER");

        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));
        Decision decision = new Decision();
        decision.setHomework(hw);
        decision.setUser(student);
        decision.setPuzzle(puzzle);
        decision.setCode("student's secret code");
        decisionRepository.save(decision);

        loginUser(realTeacher); // Логинимся как "настоящий" учитель

        mvc.perform(get("/api/homework/{id}/submissions", hw.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(decision.getId().intValue())))
                .andExpect(jsonPath("$.content[0].code", is("student's secret code"))); // Код должен быть виден
    }


    /*
     * Тестирует эндпоинт /submissions
     * Проверяет, что студент (у которого нет прав) не может видеть список решений.
     */
    @Test
    public void testGetSubmissions_Fail_AsStudent() throws Exception {
        Homework hw = homeworkRepository.save(new Homework("HW", "Desc", group, puzzle, LocalDateTime.now().plusDays(1)));

        loginUser(student); // Логинимся как студент

        mvc.perform(get("/api/homework/{id}/submissions", hw.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено переглядати здані рішення")));
    }
}
