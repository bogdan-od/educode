package com.educode.educodeApi.config;

import com.educode.educodeApi.services.RoleService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements ApplicationRunner {

    private final RoleService roleService;

    public RoleSeeder(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        roleService.initializeDefaultRoles();
    }
}
