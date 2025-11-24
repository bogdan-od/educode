package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.TreeNodeMemberDTO;
import com.educode.educodeApi.models.Role;
import com.educode.educodeApi.models.TreeNodeMember;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TreeNodeMemberMapper {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public TreeNodeMemberMapper(UserMapper userMapper, RoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * Преобразует TreeNodeMember в простой Map для ответа
     */
    public Map<String, Object> toSimpleView(TreeNodeMember member) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", member.getId());
        result.put("userId", member.getUser().getId());
        result.put("userName", member.getUser().getName());
        result.put("userEmail", member.getUser().getEmail());
        result.put("treeNodeId", member.getTreeNode().getId());
        result.put("roles", member.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        return result;
    }

    /**
     * Преобразует TreeNodeMember в детальный Map с разрешениями
     */
    public Map<String, Object> toDetailedView(TreeNodeMember member) {
        Map<String, Object> result = toSimpleView(member);
        result.put("permissions", member.getAllPermissions());
        return result;
    }

    public TreeNodeMemberDTO toViewDTO(TreeNodeMember treeNodeMember) {
        return new TreeNodeMemberDTO(treeNodeMember.getId(), userMapper.toMinViewDTO(treeNodeMember.getUser()), treeNodeMember.getRoles().stream().map(roleMapper::toDTO).toList());
    }
}
