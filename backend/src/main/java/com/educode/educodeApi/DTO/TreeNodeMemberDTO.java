package com.educode.educodeApi.DTO;

import com.educode.educodeApi.DTO.auth.RoleDTO;
import com.educode.educodeApi.DTO.user.UserDTO;

import java.util.List;

public record TreeNodeMemberDTO(
    Long id,
    UserDTO user,
    List<RoleDTO> roles
) {
}
