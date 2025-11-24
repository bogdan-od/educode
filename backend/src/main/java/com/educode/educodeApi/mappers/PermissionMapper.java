package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.auth.PermissionDTO;
import com.educode.educodeApi.enums.PermissionType;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {
    public PermissionDTO toDTO(PermissionType permissionType) {
        return new PermissionDTO(permissionType.name(), permissionType.getDescription());
    }
}
