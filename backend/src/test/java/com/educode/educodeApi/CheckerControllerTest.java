package com.educode.educodeApi;

import com.educode.educodeApi.DTO.checker.CheckerCreateDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.exceptions.ContainerBuildingException;
import com.educode.educodeApi.models.Checker;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.properties.CheckerProperties;
import com.educode.educodeApi.services.DockerClient;
import com.educode.educodeApi.utils.FileManagementUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CheckerControllerTest extends AbstractIntegrationTest {

    // Мокируем зависимости CheckerService
    @MockBean
    private DockerClient dockerClient;

    @MockBean
    private FileManagementUtil fileManagementUtil;

    @Autowired
    private CheckerProperties checkerProperties;

    private User author;
    private User otherUser;
    private MockMultipartFile validFile;

    @BeforeEach
    public void setupChecker() {
        checkerRepository.deleteAll(); // Чистим
        puzzleRepository.deleteAll();

        author = setupUser("author", PermissionType.CREATE_PUZZLES);
        otherUser = setupUser("otherUser");

        validFile = new MockMultipartFile(
                "file",
                "checker.cpp",
                "text/x-c",
                "int main() { return 0; }".getBytes()
        );

        // Мокируем успешную компиляцию по умолчанию
        try {
            when(dockerClient.compileAndSaveBinary(anyString(), anyString(), anyString(), anyString(), anyLong()))
                    .thenReturn("compiled_checker_filename_123");
            when(fileManagementUtil.getDirectorySize(any(Path.class)))
                    .thenReturn(1024L); // 1KB
        } catch (Exception e) {
            //
        }
    }

    // --- Тесты для POST /api/checker/create ---

    @Test
    public void testCreateChecker_Success() throws Exception {
        loginUser(author);

        // Устанавливаем лимит выше, чем размер файла
        checkerProperties.setBytesPerUserLimit(2048L);

        mvc.perform(MockMvcRequestBuilders.multipart("/api/checker/create")
                        .file(validFile)
                        .param("name", "Valid Checker")
                        .param("languageId", "cpp:14.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkerId").exists());

        assertEquals(1, checkerRepository.count());
    }

    @Test
    public void testCreateChecker_Fail_Unauthenticated() throws Exception {
        mvc.perform(MockMvcRequestBuilders.multipart("/api/checker/create")
                        .file(validFile)
                        .param("name", "Valid Checker")
                        .param("languageId", "cpp:14.2"))
                .andExpect(status().isUnauthorized());
    }

//    @Test
//    public void testCreateChecker_Fail_NoPermission() throws Exception {
//        loginUser(otherUser); // У otherUser ЕСТЬ права CREATE_PUZZLES
//
//        mvc.perform(MockMvcRequestBuilders.multipart("/api/checker/create")
//                        .file(validFile)
//                        .param("name", "Valid Checker")
//                        .param("languageId", "cpp:14.2"))
//                .andExpect(status().isForbidden());
//    }

    @Test
    public void testCreateChecker_Fail_CompileError() throws Exception {
        loginUser(author);

        // Мокируем ошибку компиляции
        when(dockerClient.compileAndSaveBinary(anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenThrow(new ContainerBuildingException("Compiler error output", 0));

        mvc.perform(MockMvcRequestBuilders.multipart("/api/checker/create")
                        .file(validFile)
                        .param("name", "Invalid Checker")
                        .param("languageId", "cpp:14.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Помилка при компіляції:\nCompiler error output")));
    }

    @Test
    public void testCreateChecker_Fail_StorageLimitExceeded() throws Exception {
        loginUser(author);

        // Устанавливаем лимит (100) ниже, чем вернет mock (1024)
        checkerProperties.setBytesPerUserLimit(100L);

        mvc.perform(MockMvcRequestBuilders.multipart("/api/checker/create")
                        .file(validFile)
                        .param("name", "Big Checker")
                        .param("languageId", "cpp:14.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Ви вичерпали свій ліміт")));
    }

    // --- Тесты для GET /api/checker/my ---

    @Test
    public void testGetMyCheckers_Success() throws Exception {
        loginUser(author);
        checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        checkerRepository.save(new Checker(null, author, "file2", 100, "cpp", "Checker 2"));
        // Чужой чекер
        checkerRepository.save(new Checker(null, otherUser, "file3", 100, "cpp", "Checker 3"));


        mvc.perform(get("/api/checker/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- Тесты для GET /api/checker/get/{id} ---

    @Test
    public void testGetChecker_Success_Owner() throws Exception {
        Checker checker = checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        loginUser(author);

        mvc.perform(get("/api/checker/get/{id}", checker.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(checker.getId().intValue())));
    }

    @Test
    public void testGetChecker_Fail_NotOwner() throws Exception {
        Checker checker = checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        loginUser(otherUser); // Логинимся как другой пользователь

        mvc.perform(get("/api/checker/get/{id}", checker.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Ви не можете дивитись цей checker")));
    }

    // --- Тесты для DELETE /api/checker/delete/{id} ---

    @Test
    public void testDeleteChecker_Success() throws Exception {
        Checker checker = checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        loginUser(author);

        mvc.perform(delete("/api/checker/delete/{id}", checker.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is("Checker видалено")));

        assertEquals(0, checkerRepository.count());
    }

    @Test
    public void testDeleteChecker_Fail_NotOwner() throws Exception {
        Checker checker = checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        loginUser(otherUser); // Логинимся как другой пользователь

        mvc.perform(delete("/api/checker/delete/{id}", checker.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Ви не можете видалити не свій checker")));
    }

    @Test
    public void testDeleteChecker_Fail_HasPuzzles() throws Exception {
        Checker checker = checkerRepository.save(new Checker(null, author, "file1", 100, "cpp", "Checker 1"));
        loginUser(author);

        // Создаем задачу, которая использует этот чекер
        Puzzle puzzle = createPuzzle("Puzzle with checker", author, true);
        puzzle.setChecker(checker);
        puzzleRepository.save(puzzle);

        mvc.perform(delete("/api/checker/delete/{id}", checker.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Ви не можете видалити checker, що має задачі")));
    }
}
