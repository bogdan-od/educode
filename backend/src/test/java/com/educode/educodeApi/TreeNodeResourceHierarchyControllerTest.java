package com.educode.educodeApi;

import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TreeNodeResourceHierarchyControllerTest extends AbstractIntegrationTest {

    private User puzzleAuthor;
    private User nodeAdmin;
    private Puzzle testPuzzle;
    private Node parentNode;
    private Node childNode;

    @BeforeEach
    public void setupResources() {
        // ИСПРАВЛЕНИЕ: Используем setupUser для мокирования findById
        puzzleAuthor = setupUser("pAuthor");
        nodeAdmin = setupUser("nAdmin");

        testPuzzle = createPuzzle("Test Puzzle", puzzleAuthor, false);

        parentNode = createNode("Parent Node", null);
        childNode = createNode("Child Node", parentNode.getTreeNode());
    }

    // --- Тесты на POST /{treeNodeId}/puzzles/{puzzleId}/assign ---

    @Test
    public void testAssignPuzzle_Success() throws Exception {
        // "Логиним" автора задачи И даем ему права в узле
        loginUser(puzzleAuthor);
        setupMember(parentNode.getTreeNode(), puzzleAuthor, "NODE_OWNER"); // NODE_OWNER имеет ASSIGN_PUZZLE_TO_NODE

        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Задачу успішно додано")));

        assertTrue(puzzleRepository.existsByTreeNodesContainingAndId(parentNode.getTreeNode(), testPuzzle.getId()));
    }

    @Test
    public void testAssignPuzzle_Success_ByAdmin() throws Exception {
        // "Логиним" админа (с правом PUBLISH_PUZZLE)
        // ИСПРАВЛЕНИЕ: nodeAdmin - это просто пользователь. Создадим админа.
        User admin = setupUser("admin", PermissionType.PUBLISH_PUZZLE);
        loginUser(admin);
        // Даем админу права в узле
        setupMember(parentNode.getTreeNode(), admin, "NODE_OWNER");

        // Админ назначает задачу (testPuzzle), созданную ДРУГИМ пользователем (puzzleAuthor)
        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isOk());
    }

    @Test
    public void testAssignPuzzle_Fail_NoPermissionInNode() throws Exception {
        // "Логиним" автора задачи
        loginUser(puzzleAuthor);
        // НО не даем ему прав в узле

        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено додавати задачі")));
    }

    @Test
    public void testAssignPuzzle_Fail_NotAuthorAndNotAdmin() throws Exception {
        // "Логиним" другого пользователя
        loginUser(nodeAdmin);
        // Даем ему права в узле
        setupMember(parentNode.getTreeNode(), nodeAdmin, "NODE_OWNER");

        // Но он не автор задачи и не админ (PUBLISH_PUZZLE)
        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("тільки свої задачі")));
    }

    @Test
    public void testAssignPuzzle_Fail_AlreadyAssigned() throws Exception {
        loginUser(puzzleAuthor);
        setupMember(parentNode.getTreeNode(), puzzleAuthor, "NODE_OWNER");

        // Назначаем задачу
        parentNode.getTreeNode().addPuzzle(testPuzzle);
        treeNodeRepository.save(parentNode.getTreeNode());

        // Пытаемся назначить ее снова
        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("вже додана")));
    }

    // --- Тесты на GET /{treeNodeId}/puzzles ---

    @Test
    public void testGetPuzzles_Success_Direct() throws Exception {
        loginUser(nodeAdmin);
        setupMember(parentNode.getTreeNode(), nodeAdmin, "MEMBER"); // Достаточно быть участником

        parentNode.getTreeNode().addPuzzle(testPuzzle);
        treeNodeRepository.save(parentNode.getTreeNode());

        mvc.perform(get("/api/tree-node/{treeNodeId}/puzzles", parentNode.getTreeNode().getId())
                        .param("includeInherited", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.puzzles[0].id", is(testPuzzle.getId().intValue())));
    }

    @Test
    public void testGetPuzzles_Success_Inherited() throws Exception {
        loginUser(nodeAdmin);
        // Делаем участником дочернего узла
        setupMember(childNode.getTreeNode(), nodeAdmin, "MEMBER");

        // Добавляем задачу в РОДИТЕЛЬСКИЙ узел
        parentNode.getTreeNode().addPuzzle(testPuzzle);
        treeNodeRepository.save(parentNode.getTreeNode());

        // Запрашиваем задачи ДОЧЕРНЕГО узла с наследованием
        mvc.perform(get("/api/tree-node/{treeNodeId}/puzzles", childNode.getTreeNode().getId())
                        .param("includeInherited", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.puzzles[0].id", is(testPuzzle.getId().intValue())));
    }

    @Test
    public void testGetPuzzles_Success_NoInheritance() throws Exception {
        loginUser(nodeAdmin);
        setupMember(childNode.getTreeNode(), nodeAdmin, "MEMBER");

        parentNode.getTreeNode().addPuzzle(testPuzzle);
        treeNodeRepository.save(parentNode.getTreeNode());

        // Запрашиваем задачи ДОЧЕРНЕГО узла БЕЗ наследования
        mvc.perform(get("/api/tree-node/{treeNodeId}/puzzles", childNode.getTreeNode().getId())
                        .param("includeInherited", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // --- Тесты на PUT /{treeNodeId}/move ---

    @Test
    public void testMoveNode_Success() throws Exception {
        loginUser(nodeAdmin);
        setupMember(childNode.getTreeNode(), nodeAdmin, "NODE_OWNER"); // Права на перемещение

        Node newParent = createNode("New Parent", null);
        // Права на добавление в новый узел
        setupMember(newParent.getTreeNode(), nodeAdmin, "NODE_OWNER");

        mvc.perform(put("/api/tree-node/{treeNodeId}/move", childNode.getTreeNode().getId())
                        .param("newParentId", newParent.getTreeNode().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newParentId", is(newParent.getTreeNode().getId().intValue())));

        TreeNode updatedChild = treeNodeRepository.findById(childNode.getTreeNode().getId()).get();
        assertEquals(newParent.getTreeNode().getId(), updatedChild.getParent().getId());
    }

    @Test
    public void testMoveNode_Fail_Cycle() throws Exception {
        loginUser(nodeAdmin);
        setupMember(parentNode.getTreeNode(), nodeAdmin, "NODE_OWNER"); // Права на перемещение
        setupMember(childNode.getTreeNode(), nodeAdmin, "NODE_OWNER"); // Права на добавление

        // Пытаемся переместить parentNode (родителя) внутрь childNode (потомка)
        mvc.perform(put("/api/tree-node/{treeNodeId}/move", parentNode.getTreeNode().getId())
                        .param("newParentId", childNode.getTreeNode().getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Неможливо перемістити")));
    }

    @Test
    public void testMoveNode_Fail_NoPermissionToMove() throws Exception {
        loginUser(nodeAdmin);
        // Даем права "STUDENT" (недостаточно)
        setupMember(childNode.getTreeNode(), nodeAdmin, "STUDENT");

        Node newParent = createNode("New Parent", null);
        setupMember(newParent.getTreeNode(), nodeAdmin, "NODE_OWNER");

        mvc.perform(put("/api/tree-node/{treeNodeId}/move", childNode.getTreeNode().getId())
                        .param("newParentId", newParent.getTreeNode().getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено переміщувати")));
    }

    @Test
    public void testMoveNode_Fail_NoPermissionInTarget() throws Exception {
        loginUser(nodeAdmin);
        setupMember(childNode.getTreeNode(), nodeAdmin, "NODE_OWNER"); // Права на перемещение есть

        Node newParent = createNode("New Parent", null);
        // НЕТ прав на добавление в newParent (не делаем setupMember)

        mvc.perform(put("/api/tree-node/{treeNodeId}/move", childNode.getTreeNode().getId())
                        .param("newParentId", newParent.getTreeNode().getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено додавати вузли")));
    }

    /*
     * Тестирует, что администратор (с правом PUBLISH_PUZZLE)
     * может добавить ЧУЖУЮ задачу в узел, если у него есть права
     * ASSIGN_PUZZLE_TO_NODE в этом узле.
     */
    @Test
    public void testAssignPuzzle_Success_ByAdminNonAuthor() throws Exception {
        // 1. Создаем админа
        User admin = setupUser("admin", PermissionType.PUBLISH_PUZZLE);
        // 2. Логинимся как админ
        loginUser(admin);
        // 3. Даем админу права в узле
        setupMember(parentNode.getTreeNode(), admin, "NODE_OWNER"); // NODE_OWNER имеет ASSIGN_PUZZLE_TO_NODE

        // (testPuzzle был создан 'puzzleAuthor' в @BeforeEach)

        // 4. Админ назначает ЧУЖУЮ задачу
        mvc.perform(post("/api/tree-node/{treeNodeId}/puzzles/{puzzleId}/assign",
                        parentNode.getTreeNode().getId(), testPuzzle.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Задачу успішно додано")));
    }
}
