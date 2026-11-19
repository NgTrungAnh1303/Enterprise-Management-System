package com.fpt.ems.be.config;

import com.fpt.ems.be.model.Role;
import com.fpt.ems.be.model.enums.RoleName;
import com.fpt.ems.be.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RoleDataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                log.info("Auto-initializing missing system role: {}", roleName);
                roleRepository.save(Role.builder()
                        .name(roleName)
                        .build());
            }
        }
    }
}
