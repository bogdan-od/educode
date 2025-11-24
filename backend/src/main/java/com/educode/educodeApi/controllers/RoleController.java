package com.educode.educodeApi.controllers;

import com.educode.educodeApi.DTO.auth.RoleDTO;
import com.educode.educodeApi.DTO.auth.RoleDetailDTO;
import com.educode.educodeApi.enums.RoleScope;
import com.educode.educodeApi.exceptions.NotFoundError;
import com.educode.educodeApi.mappers.RoleMapper;
import com.educode.educodeApi.models.Role;
import com.educode.educodeApi.repositories.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleController(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @GetMapping("")
    public ResponseEntity<List<RoleDTO>> getAllRoles(
            @RequestParam(name = "scope", required = false) RoleScope roleScope
    ) {
        if (roleScope == null)
            return ResponseEntity.ok(roleRepository.findAll().stream().map(roleMapper::toDTO).toList());
        else {
            return ResponseEntity.ok(roleRepository.findAllByScopeIn(roleScope.getChildren()).stream().map(roleMapper::toDTO).toList());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDetailDTO> get(@PathVariable Long id) {
        Role role = roleRepository.findById(id).orElse(null);
        if (role == null)
            throw new NotFoundError();

        return ResponseEntity.ok(roleMapper.toDetailDTO(role));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleDetailDTO> get(@PathVariable String name) {
        Role role = roleRepository.findByName(name).orElse(null);
        if (role == null)
            throw new NotFoundError();

        return ResponseEntity.ok(roleMapper.toDetailDTO(role));
    }
}
