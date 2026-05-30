package com.app.backend.service.security;

import com.app.backend.entity.Permission;
import com.app.backend.entity.User;
import com.app.backend.entity.UserPermission;
import com.app.backend.repository.PermissionRepository;
import com.app.backend.repository.UserPermissionRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionAssignmentService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Transactional
    public void replacePermissions(Long userId, Set<String> permissionCodes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userPermissionRepository.deleteAllByUserId(userId);
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }
        for (String code : permissionCodes) {
            Permission perm = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown permission: " + code));
            UserPermission link = new UserPermission();
            link.setUser(user);
            link.setPermission(perm);
            userPermissionRepository.save(link);
        }
    }
}
