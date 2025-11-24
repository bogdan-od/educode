package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.auth.MinimalRoleDTO;
import com.educode.educodeApi.DTO.auth.RoleDTO;
import com.educode.educodeApi.DTO.auth.RoleDetailDTO;
import com.educode.educodeApi.models.Role;
import com.educode.educodeApi.utils.MappingUtils;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {
    private final PermissionMapper permissionMapper;

    public RoleMapper(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public MinimalRoleDTO toMinimalDTO(Role role) {
        return new MinimalRoleDTO(role.getName(), role.getDescription());
    }

    public RoleDTO toDTO(Role role) {
        return new RoleDTO(role.getName(), role.getDescription(), role.getPriority(), role.getScope());
    }

    public RoleDetailDTO toDetailDTO(Role role) {
        return new RoleDetailDTO(role.getName(), role.getDescription(), role.getScope(), role.getPriority(), MappingUtils.toDtoList(role.getPermissions(), permissionMapper::toDTO));
    }
}
