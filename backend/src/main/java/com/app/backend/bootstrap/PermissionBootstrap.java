package com.app.backend.bootstrap;

import com.app.backend.entity.Permission;
import com.app.backend.entity.PermissionModule;
import com.app.backend.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.app.backend.security.PermissionCodes.ASSIGN_AGENT;
import static com.app.backend.security.PermissionCodes.OWNER_EDIT;
import static com.app.backend.security.PermissionCodes.OWNER_VIEW;
import static com.app.backend.security.PermissionCodes.PLOT_DELETE;
import static com.app.backend.security.PermissionCodes.USER_MANAGE;
import static com.app.backend.security.PermissionCodes.VIEW_ACTIVITY_LOGS;

/**
 * Ensures canonical permission rows exist (idempotent).
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class PermissionBootstrap implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsert(OWNER_VIEW, "View owner registry and sensitive owner-linked data", PermissionModule.OWNER);
        upsert(OWNER_EDIT, "Create, update, or delete registry owners", PermissionModule.OWNER);
        upsert(PLOT_DELETE, "Delete plots (with ownership rules for agents)", PermissionModule.PLOT);
        upsert(USER_MANAGE, "Create users and manage accounts", PermissionModule.USER);
        upsert(ASSIGN_AGENT, "Assign elevated permissions to users", PermissionModule.USER);
        upsert(VIEW_ACTIVITY_LOGS, "View global activity logs", PermissionModule.SYSTEM);
        log.debug("Permission catalog verified");
    }

    private void upsert(String code, String description, PermissionModule module) {
        permissionRepository.findByCode(code).orElseGet(() -> {
            Permission p = new Permission();
            p.setCode(code);
            p.setDescription(description);
            p.setModule(module);
            return permissionRepository.save(p);
        });
    }
}
