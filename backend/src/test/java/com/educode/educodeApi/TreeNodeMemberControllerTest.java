package com.educode.educodeApi;

import com.educode.educodeApi.DTO.treenode.AddMemberDTO;
import com.educode.educodeApi.DTO.treenode.UpdateMemberRolesDTO;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TreeNodeMemberControllerTest extends AbstractIntegrationTest {

    private User adminUser;
    private User targetUser;
    private Node testNode;

    @BeforeEach
    public void setupMembers() {
        // ИСПРАВЛЕНИЕ: Используем setupUser для создания/поиска И мокирования findById
        adminUser = setupUser("admin");
        targetUser = setupUser("target");

        testNode = createNode("Test Node", null);
    }

    // --- Тесты на POST /{treeNodeId}/members/add ---

    @Test
    public void testAddMember_Success() throws Exception {
        // "Логиним" adminUser и даем ему права в узле
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER"); // Даем права

        AddMemberDTO dto = new AddMemberDTO(targetUser.getId(), Set.of("STUDENT"));

        mvc.perform(post("/api/tree-node/{treeNodeId}/members/add", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Користувача успішно додано")));

        assertTrue(memberRepository.existsByUserAndTreeNode(targetUser, testNode.getTreeNode()));
    }

    @Test
    public void testAddMember_Fail_Unauthenticated() throws Exception {
        // Не "логиним" никого (loginUser не вызывается)
        AddMemberDTO dto = new AddMemberDTO(targetUser.getId(), Set.of("STUDENT"));

        mvc.perform(post("/api/tree-node/{treeNodeId}/members/add", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAddMember_Fail_NoPermission() throws Exception {
        // "Логиним" adminUser, но НЕ даем ему прав в узле (он просто "STUDENT")
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "STUDENT");

        AddMemberDTO dto = new AddMemberDTO(targetUser.getId(), Set.of("STUDENT"));

        mvc.perform(post("/api/tree-node/{treeNodeId}/members/add", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено додавати користувачів")));
    }

    @Test
    public void testAddMember_Fail_AlreadyMember() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");

        // targetUser УЖЕ является участником
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT");

        AddMemberDTO dto = new AddMemberDTO(targetUser.getId(), Set.of("STUDENT"));

        mvc.perform(post("/api/tree-node/{treeNodeId}/members/add", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("вже є учасником")));
    }

    @Test
    public void testAddMember_Fail_UserNotFound() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");

        AddMemberDTO dto = new AddMemberDTO(9999L, Set.of("STUDENT")); // Несуществующий ID

        // (userService.findById(9999L) вернет null, т.к. не был замокирован)
        mvc.perform(post("/api/tree-node/{treeNodeId}/members/add", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()) // 400
                .andExpect(jsonPath("$.error", containsString("Не знайдено користувача")));
    }

    // --- Тесты на DELETE /{treeNodeId}/members/{userId} ---

    @Test
    public void testRemoveMember_Success() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT"); // Добавляем пользователя для удаления

        assertTrue(memberRepository.existsByUserAndTreeNode(targetUser, testNode.getTreeNode()));

        mvc.perform(delete("/api/tree-node/{treeNodeId}/members/{userId}",
                        testNode.getTreeNode().getId(), targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Користувача успішно видалено з вузла")));

        assertFalse(memberRepository.existsByUserAndTreeNode(targetUser, testNode.getTreeNode()));
    }

    @Test
    public void testRemoveMember_Fail_NoPermission() throws Exception {
        loginUser(adminUser);
        // Даем права "STUDENT" (недостаточно для удаления)
        setupMember(testNode.getTreeNode(), adminUser, "STUDENT");
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT");

        mvc.perform(delete("/api/tree-node/{treeNodeId}/members/{userId}",
                        testNode.getTreeNode().getId(), targetUser.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено видаляти")));
    }

    @Test
    public void testRemoveMember_Fail_LastOwner() throws Exception {
        loginUser(adminUser);
        // adminUser - ЕДИНСТВЕННЫЙ владелец
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");

        assertEquals(1, memberRepository.countMembersWithRole(testNode.getTreeNode().getId(), "NODE_OWNER"));

        // Пытаемся удалить сами себя
        // ИСПРАВЛЕНИЕ: `findById(adminUser.getId())` теперь работает благодаря `setupUser`
        mvc.perform(delete("/api/tree-node/{treeNodeId}/members/{userId}",
                        testNode.getTreeNode().getId(), adminUser.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("останнього адміністратора")));
    }

    @Test
    public void testRemoveMember_Success_RemoveSelfAsNonLastOwner() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");

        // Добавляем второго владельца
        User otherOwner = setupUser("other"); // ИСПРАВЛЕНО: setupUser мокирует findById
        setupMember(testNode.getTreeNode(), otherOwner, "NODE_OWNER");

        assertEquals(2, memberRepository.countMembersWithRole(testNode.getTreeNode().getId(), "NODE_OWNER"));

        // Пытаемся удалить сами себя
        mvc.perform(delete("/api/tree-node/{treeNodeId}/members/{userId}",
                        testNode.getTreeNode().getId(), adminUser.getId()))
                .andExpect(status().isOk());

        assertEquals(1, memberRepository.countMembersWithRole(testNode.getTreeNode().getId(), "NODE_OWNER"));
    }

    // --- Тесты на PUT /{treeNodeId}/members/{userId}/roles ---

    @Test
    public void testUpdateMemberRoles_Success() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER"); // У adminUser есть права
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT"); // targetUser - студент

        UpdateMemberRolesDTO dto = new UpdateMemberRolesDTO(Set.of("TEACHER"));

        // ИСПРАВЛЕНИЕ: `findById(targetUser.getId())` теперь работает
        mvc.perform(put("/api/tree-node/{treeNodeId}/members/{userId}/roles",
                        testNode.getTreeNode().getId(), targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("TEACHER")));

        var member = memberRepository.findByUserAndTreeNode(targetUser, testNode.getTreeNode());
        assertTrue(member.hasRole("TEACHER"));
        assertFalse(member.hasRole("STUDENT"));
    }

    @Test
    public void testUpdateMemberRoles_Fail_HighPriority() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT");

        UpdateMemberRolesDTO dto = new UpdateMemberRolesDTO(Set.of("ADMIN"));

        mvc.perform(put("/api/tree-node/{treeNodeId}/members/{userId}/roles",
                        testNode.getTreeNode().getId(), targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("пріоритет вище")));
    }

    @Test
    public void testUpdateMemberRoles_Success_HighPriorityAncestor() throws Exception {
        loginUser(adminUser);
        TreeNode childNode = createNode("Child Node", testNode.getTreeNode()).getTreeNode();
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");
        setupMember(childNode, targetUser, "STUDENT");

        UpdateMemberRolesDTO dto = new UpdateMemberRolesDTO(Set.of("NODE_OWNER"));

        mvc.perform(put("/api/tree-node/{treeNodeId}/members/{userId}/roles",
                        childNode.getId(), targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        var member = memberRepository.findByUserAndTreeNode(targetUser, childNode);
        assertTrue(member.hasRole("NODE_OWNER"));
        assertFalse(member.hasRole("STUDENT"));
    }

    @Test
    public void testUpdateMemberRoles_Fail_NoPermission() throws Exception {
        loginUser(adminUser);
        setupMember(testNode.getTreeNode(), adminUser, "STUDENT"); // У adminUser НЕТ прав
        setupMember(testNode.getTreeNode(), targetUser, "STUDENT");

        UpdateMemberRolesDTO dto = new UpdateMemberRolesDTO(Set.of("TEACHER"));

        mvc.perform(put("/api/tree-node/{treeNodeId}/members/{userId}/roles",
                        testNode.getTreeNode().getId(), targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено змінювати ролі")));
    }

    @Test
    public void testUpdateMemberRoles_Fail_RemoveLastOwnerRole() throws Exception {
        loginUser(adminUser);
        // adminUser - единственный владелец
        setupMember(testNode.getTreeNode(), adminUser, "NODE_OWNER");

        // Пытаемся снять с себя роль владельца
        UpdateMemberRolesDTO dto = new UpdateMemberRolesDTO(Set.of("STUDENT"));

        // ИСПРАВЛЕНИЕ: `findById(adminUser.getId())` теперь работает
        mvc.perform(put("/api/tree-node/{treeNodeId}/members/{userId}/roles",
                        testNode.getTreeNode().getId(), adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("останнього адміністратора")));
    }
}
