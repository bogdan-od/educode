package com.educode.educodeApi;

import com.educode.educodeApi.DTO.treenode.invitation.InvitationCreateDTO;
import com.educode.educodeApi.DTO.treenode.invitation.InvitationUpdateDTO;
import com.educode.educodeApi.enums.InvitationType;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.TreeNode;
import com.educode.educodeApi.models.TreeNodeInvitation;
import com.educode.educodeApi.models.User;
import com.educode.educodeApi.repositories.TreeNodeInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TreeNodeInvitationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TreeNodeInvitationRepository invitationRepository;

    private User owner;
    private User student1;
    private User student2;
    private User otherNodeUser;
    private Node testNode;
    private Node otherNode;

    @BeforeEach
    public void setupInvitations() {
        invitationRepository.deleteAll();

        owner = setupUser("inv_owner");
        student1 = setupUser("inv_student1");
        student2 = setupUser("inv_student2");
        otherNodeUser = setupUser("inv_othernode_user");

        testNode = createNode("Invitation Node", null);
        otherNode = createNode("Other Node", null);

        // Владелец имеет права на INVITE_USERS
        setupMember(testNode.getTreeNode(), owner, "NODE_OWNER");
        // student2 - обычный участник без прав
        setupMember(testNode.getTreeNode(), student2, "STUDENT");
        // otherNodeUser - участник другого узла
        setupMember(otherNode.getTreeNode(), otherNodeUser, "STUDENT");
    }

    // --- Тесты на POST /tree-node/{treeNodeId}/invitations (Create) ---

    @Test
    public void testCreateInvitation_Success_Public() throws Exception {
        loginUser(owner);

        InvitationCreateDTO dto = new InvitationCreateDTO(
                Set.of("STUDENT"),
                null, // Вечное
                InvitationType.PUBLIC,
                null,
                Collections.emptySet()
        );

        mvc.perform(post("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.invitationType", is("PUBLIC")))
                .andExpect(jsonPath("$.expiresAt", is(nullValue())));

        assertEquals(1, invitationRepository.count());
    }

    @Test
    public void testCreateInvitation_Success_SingleUse() throws Exception {
        loginUser(owner);

        InvitationCreateDTO dto = new InvitationCreateDTO(
                Set.of("STUDENT"),
                LocalDateTime.now().plusDays(1),
                InvitationType.SINGLE_USE,
                null,
                Set.of(student1.getId()) // Для student1
        );

        mvc.perform(post("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationType", is("SINGLE_USE")))
                .andExpect(jsonPath("$.userCount", is(1)));
    }

    @Test
    public void testCreateInvitation_Success_NodeBased() throws Exception {
        loginUser(owner);

        InvitationCreateDTO dto = new InvitationCreateDTO(
                Set.of("STUDENT"),
                null,
                InvitationType.NODE_BASED,
                otherNode.getTreeNode().getId(), // Только для участников otherNode
                Collections.emptySet()
        );

        mvc.perform(post("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationType", is("NODE_BASED")))
                .andExpect(jsonPath("$.allowedTreeNodeId", is(otherNode.getTreeNode().getId().intValue())));
    }

    @Test
    public void testCreateInvitation_Fail_NoPermission() throws Exception {
        loginUser(student2); // У student2 нет прав INVITE_USERS

        InvitationCreateDTO dto = new InvitationCreateDTO(
                Set.of("STUDENT"), null, InvitationType.PUBLIC, null, Collections.emptySet()
        );

        mvc.perform(post("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("У вас немає прав для створення запрошень")));
    }

    @Test
    public void testCreateInvitation_Fail_ValidationError_SingleUseMultipleUsers() throws Exception {
        loginUser(owner);

        InvitationCreateDTO dto = new InvitationCreateDTO(
                Set.of("STUDENT"),
                null,
                InvitationType.SINGLE_USE,
                null,
                Set.of(student1.getId(), student2.getId()) // Ошибка: 2 пользователя
        );

        mvc.perform(post("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Для типу SINGLE_USE потрібен рівно 1 користувач")));
    }

    // --- Тесты на POST /invitations/accept/{code} (Accept) ---

    @Test
    public void testAcceptInvitation_Success_Public() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);

        loginUser(student1); // Логинимся как студент, который еще не в группе

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Ви успішно приєдналися!")));

        assertTrue(memberService.isMemberOfNode(student1, testNode.getTreeNode()));
    }

    @Test
    public void testAcceptInvitation_Fail_AlreadyMember() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);

        loginUser(student2); // student2 УЖЕ в группе

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Ви вже приєдналися сюди")));
    }

    @Test
    public void testAcceptInvitation_Fail_Expired() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);
        inv.setExpiresAt(LocalDateTime.now().minusDays(1)); // Делаем просроченным
        invitationRepository.save(inv);

        loginUser(student1);

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Запрошення не активне")));
    }

    @Test
    public void testAcceptInvitation_Fail_LimitedList_UserNotInList() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.LIMITED_LIST, Set.of(student2), null); // Только для student2

        loginUser(student1); // Пытается войти student1

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Вам не дозволено приєднуватися")));
    }

    @Test
    public void testAcceptInvitation_Success_SingleUse_And_Fail_SecondUse() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.SINGLE_USE, Set.of(student1), null); // Только для student1

        loginUser(student1); // student1 принимает

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isOk());

        // Приглашение должно быть удалено
        assertFalse(invitationRepository.findByCode(inv.getCode()) != null);

        // Второй пользователь (или тот же) пытается использовать
        loginUser(student2);
        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Не знайдено такого запрошення")));
    }

    @Test
    public void testAcceptInvitation_Success_NodeBased() throws Exception {
        loginUser(owner);
        // Приглашение для тех, кто в otherNode
        TreeNodeInvitation inv = createInvite(InvitationType.NODE_BASED, null, otherNode.getTreeNode());

        // otherNodeUser состоит в otherNode
        loginUser(otherNodeUser);

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isOk());

        // Проверяем, что он стал участником testNode
        assertTrue(memberService.isMemberOfNode(otherNodeUser, testNode.getTreeNode()));
    }

    @Test
    public void testAcceptInvitation_Fail_NodeBased_NotMember() throws Exception {
        loginUser(owner);
        // Приглашение для тех, кто в otherNode
        TreeNodeInvitation inv = createInvite(InvitationType.NODE_BASED, null, otherNode.getTreeNode());

        // student1 НЕ состоит в otherNode
        loginUser(student1);

        mvc.perform(post("/api/invitations/accept/{code}", inv.getCode()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Вам не дозволено приєднуватися")));
    }

    // --- Тесты на GET /{treeNodeId}/invitations (List) ---

    @Test
    public void testGetInvitations_Success_Sorting() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv1 = createInvite(InvitationType.PUBLIC, null, null); // id 1 (старый)
        TreeNodeInvitation inv2 = createInvite(InvitationType.PUBLIC, null, null); // id 2 (новый)

        mvc.perform(get("/api/tree-node/{treeNodeId}/invitations", testNode.getTreeNode().getId())
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations", hasSize(2)))
                .andExpect(jsonPath("$.invitations[0].id", is(inv1.getId().intValue()))) // Сортировка по ASC id
                .andExpect(jsonPath("$.invitations[1].id", is(inv2.getId().intValue())));
    }

    // --- Тесты на DELETE /{treeNodeId}/invitations/{invitationId} (Delete) ---

    @Test
    public void testDeleteInvitation_Success_Owner() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);

        assertTrue(invitationRepository.findById(inv.getId()).isPresent());

        mvc.perform(delete("/api/tree-node/{treeNodeId}/invitations/{invitationId}",
                        testNode.getTreeNode().getId(), inv.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Запрошення успішно видалено")));

        assertFalse(invitationRepository.findById(inv.getId()).isPresent());
    }

    @Test
    public void testDeleteInvitation_Fail_NotOwner() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);

        loginUser(student2); // Логинимся как студент

        mvc.perform(delete("/api/tree-node/{treeNodeId}/invitations/{invitationId}",
                        testNode.getTreeNode().getId(), inv.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("У вас немає прав для видалення запрошень")));
    }

    // --- Тесты на PUT /{treeNodeId}/invitations/{invitationId} (Update) ---

    @Test
    public void testUpdateInvitation_Success_Deactivate() throws Exception {
        loginUser(owner);
        TreeNodeInvitation inv = createInvite(InvitationType.PUBLIC, null, null);
        assertTrue(inv.getActive());

        InvitationUpdateDTO dto = new InvitationUpdateDTO(null, null, false); // Деактивировать

        mvc.perform(put("/api/tree-node/{treeNodeId}/invitations/{invitationId}",
                        testNode.getTreeNode().getId(), inv.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        assertFalse(invitationRepository.findById(inv.getId()).get().getActive());
    }


    /** Вспомогательный метод для создания приглашения в тестах */
    private TreeNodeInvitation createInvite(InvitationType type, Set<User> users, TreeNode allowedNode) {
        return invitationRepository.save(new TreeNodeInvitation(
                testNode.getTreeNode(),
                UUID.randomUUID().toString(),
                users,
                Set.of(roleService.findByType("STUDENT")),
                null,
                type,
                allowedNode
        ));
    }
}
