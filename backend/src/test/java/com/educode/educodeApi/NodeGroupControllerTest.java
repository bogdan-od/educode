package com.educode.educodeApi;

import com.educode.educodeApi.DTO.group.GroupCreateDTO;
import com.educode.educodeApi.DTO.node.NodeCreateDTO;
import com.educode.educodeApi.enums.PermissionType;
import com.educode.educodeApi.models.Group;
import com.educode.educodeApi.models.Node;
import com.educode.educodeApi.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class NodeGroupControllerTest extends AbstractIntegrationTest {

    // --- Тесты для NodeController ---

    @Test
    public void testCreateNode_Success_Root() throws Exception {
        // "Логиним" пользователя с глобальным правом CREATE_NODES
        User creator = setupUser("nodeCreator", PermissionType.CREATE_NODES);
        loginUser(creator);

        NodeCreateDTO dto = new NodeCreateDTO("Root Node", "Root desc", null);

        mvc.perform(post("/api/node/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Вузол успішно створено")))
                .andExpect(jsonPath("$.nodeId", notNullValue()));

        Node node = nodeRepository.findAll().get(0);
        assertEquals("Root Node", node.getTitle());
        assertNull(node.getTreeNode().getParent());

        // Проверяем, что создатель стал владельцем
        assertTrue(memberRepository.existsByUserAndTreeNode(creator, node.getTreeNode()));
        assertTrue(memberRepository.findByUserAndTreeNode(creator, node.getTreeNode()).hasRole("NODE_OWNER"));
    }

    @Test
    public void testCreateNode_Success_Child() throws Exception {
        User creator = setupUser("nodeCreator");
        loginUser(creator);
        Node parent = createNode("Parent Node", null);

        // Даем права на создание в родителе
        setupMember(parent.getTreeNode(), creator, "NODE_OWNER"); // NODE_OWNER имеет CREATE_NODES

        NodeCreateDTO dto = new NodeCreateDTO("Child Node", "Child desc", parent.getId());

        mvc.perform(post("/api/node/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        Node child = nodeRepository.findByTreeNode(treeNodeRepository.findAllByParent(parent.getTreeNode()).get(0));
        assertEquals("Child Node", child.getTitle());
        assertEquals(parent.getTreeNode().getId(), child.getTreeNode().getParent().getId());
    }

    @Test
    public void testCreateNode_Fail_NoPermission_Root() throws Exception {
        User user = setupUser("noPermUser"); // Без права CREATE_NODES
        loginUser(user);

        NodeCreateDTO dto = new NodeCreateDTO("Root Node", "Root desc", null);

        mvc.perform(post("/api/node/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено створювати корневі вузли")));
    }

    @Test
    public void testCreateNode_Fail_NoPermission_Child() throws Exception {
        User creator = setupUser("noPermUser"); // Без прав
        loginUser(creator);
        Node parent = createNode("Parent Node", null);

        // Не даем прав в родителе

        NodeCreateDTO dto = new NodeCreateDTO("Child Node", "Child desc", parent.getId());

        mvc.perform(post("/api/node/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("не дозволено створювати тут вузли")));
    }

    @Test
    public void testDeleteNode_Fail_HasChildren() throws Exception {
        User owner = setupUser("owner");
        loginUser(owner);
        Node parent = createNode("Parent Node", null);
        Node child = createNode("Child Node", parent.getTreeNode());

        setupMember(parent.getTreeNode(), owner, "NODE_OWNER"); // Даем права на удаление

        assertTrue(treeNodeRepository.existsByParent(parent.getTreeNode()), "У родителя должен быть дочерний элемент");

        mvc.perform(delete("/api/node/{id}", parent.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("містить дочірні елементи")));
    }

    @Test
    public void testDeleteNode_Success() throws Exception {
        User owner = setupUser("owner");
        loginUser(owner);
        Node child = createNode("Child Node", null); // Без детей

        setupMember(child.getTreeNode(), owner, "NODE_OWNER");

        assertFalse(treeNodeRepository.existsByParent(child.getTreeNode())); // ИСПРАВЛЕНО: Проверка через репо

        mvc.perform(delete("/api/node/{id}", child.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Вузол успішно видалено")));

        assertEquals(0, nodeRepository.count());
        assertEquals(0, treeNodeRepository.count()); // Удалился каскадно
    }

    // --- Тесты для GroupController ---

    @Test
    public void testCreateGroup_Success() throws Exception {
        User creator = setupUser("groupCreator");
        loginUser(creator);
        Node parent = createNode("Parent Node", null);

        // Даем права на создание групп в родителе
        setupMember(parent.getTreeNode(), creator, "NODE_OWNER"); // NODE_OWNER имеет CREATE_GROUPS

        GroupCreateDTO dto = new GroupCreateDTO("Test Group", "Group desc", parent.getId());

        mvc.perform(post("/api/group/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Групу успішно створено")));

        Group group = groupRepository.findAll().get(0);
        assertEquals("Test Group", group.getTitle());
        assertEquals(parent.getTreeNode().getId(), group.getTreeNode().getParent().getId());
        assertFalse(group.getTreeNode().getCanHaveChildren()); // Проверяем флаг

        // Проверяем, что создатель стал GROUP_OWNER
        assertTrue(memberRepository.findByUserAndTreeNode(creator, group.getTreeNode()).hasRole("GROUP_OWNER"));
    }

    @Test
    public void testDeleteGroup_Success() throws Exception {
        User owner = setupUser("owner");
        loginUser(owner);
        Group group = createGroup("Test Group", null); // Без детей (группы не могут их иметь)

        setupMember(group.getTreeNode(), owner, "GROUP_OWNER"); // Даем права

        mvc.perform(delete("/api/group/{id}", group.getId()))
                .andExpect(status().isOk());

        assertEquals(0, groupRepository.count());
    }
}
