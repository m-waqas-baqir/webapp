package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.admin.ManagedUserDetailResponse;
import com.app.backend.dto.admin.ManagedUserListItemResponse;
import com.app.backend.dto.admin.UserCreateRequest;
import com.app.backend.dto.admin.UserPermissionsRequest;
import com.app.backend.dto.admin.UserUpdateRequest;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.UserPermissionRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.UserManagementService;
import com.app.backend.service.security.PermissionAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionAssignmentService permissionAssignmentService;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManagedUserListItemResponse> list(UserPrincipal actor, Pageable pageable) {
        assertStaff(actor);
        return PageResponse.from(userRepository.findAll(pageable).map(u -> ManagedUserListItemResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .active(u.isActive())
                .permissionCount(userPermissionRepository.countByUser_Id(u.getId()))
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedUserDetailResponse get(UserPrincipal actor, Long id) {
        assertStaff(actor);
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertCanManageTarget(actor, u);
        return toDetail(u);
    }

    @Override
    @Transactional
    public ManagedUserDetailResponse create(UserPrincipal actor, UserCreateRequest request) {
        assertStaff(actor);
        validateRoleCreate(actor, request.getRole());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User u = new User();
        u.setName(request.getName().trim());
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(request.getPassword()));
        u.setRole(request.getRole());
        u.setActive(true);
        u = userRepository.save(u);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.USER, u.getId());

        Set<String> codes = sanitizePermissionsForActor(actor, request.getPermissionCodes());
        permissionAssignmentService.replacePermissions(u.getId(), codes);
        return toDetail(userRepository.findById(u.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public ManagedUserDetailResponse update(UserPrincipal actor, Long id, UserUpdateRequest request) {
        assertStaff(actor);
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertCanManageTarget(actor, u);
        if (request.getRole() != null) {
            validateRoleTransition(actor, u, request.getRole());
            u.setRole(request.getRole());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            u.setName(request.getName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.equalsIgnoreCase(u.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new IllegalArgumentException("Email already in use");
            }
            u.setEmail(email);
        }
        if (request.getActive() != null) {
            if (Boolean.FALSE.equals(request.getActive()) && actor.getRole() == UserRole.ADMIN
                    && u.getRole() == UserRole.DIRECTOR) {
                throw new AccessDeniedException("Only a director can deactivate another director");
            }
            u.setActive(request.getActive());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        u = userRepository.save(u);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.USER, u.getId());
        return toDetail(u);
    }

    @Override
    @Transactional
    public void deactivate(UserPrincipal actor, Long id) {
        UserUpdateRequest r = new UserUpdateRequest();
        r.setActive(false);
        update(actor, id, r);
    }

    @Override
    @Transactional
    public void replacePermissions(UserPrincipal actor, Long id, UserPermissionsRequest request) {
        assertStaff(actor);
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertCanManageTarget(actor, u);
        if (!canAssignElevatedPermissions(actor)) {
            Set<String> forbidden = Set.of(PermissionCodes.USER_MANAGE, PermissionCodes.ASSIGN_AGENT);
            for (String c : request.getPermissionCodes()) {
                if (forbidden.contains(c)) {
                    throw new AccessDeniedException("Only DIRECTOR can assign permission: " + c);
                }
            }
        }
        permissionAssignmentService.replacePermissions(id, sanitizePermissionsForActor(actor, request.getPermissionCodes()));
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.USER, id);
    }

    private ManagedUserDetailResponse toDetail(User u) {
        Set<String> codes = new HashSet<>(userPermissionRepository.findPermissionCodesByUserId(u.getId()));
        return ManagedUserDetailResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .active(u.isActive())
                .permissions(codes)
                .build();
    }

    private static void assertStaff(UserPrincipal actor) {
        if (actor.getRole() != UserRole.DIRECTOR && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Insufficient role");
        }
    }

    private void assertCanManageTarget(UserPrincipal actor, User target) {
        if (actor.getRole() == UserRole.ADMIN && target.getRole() == UserRole.DIRECTOR) {
            throw new AccessDeniedException("Admins cannot manage director accounts");
        }
    }

    private void validateRoleCreate(UserPrincipal actor, UserRole newRole) {
        if (actor.getRole() == UserRole.ADMIN && (newRole == UserRole.DIRECTOR)) {
            throw new AccessDeniedException("Admins cannot create director accounts");
        }
    }

    private void validateRoleTransition(UserPrincipal actor, User target, UserRole newRole) {
        if (actor.getRole() == UserRole.ADMIN) {
            if (newRole == UserRole.DIRECTOR) {
                throw new AccessDeniedException("Admins cannot promote users to director");
            }
        }
    }

    private boolean canAssignElevatedPermissions(UserPrincipal actor) {
        return actor.getRole() == UserRole.DIRECTOR;
    }

    /** ADMIN cannot assign privileged codes even if they slip through the UI. */
    private Set<String> sanitizePermissionsForActor(UserPrincipal actor, Set<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return Set.of();
        }
        if (actor.getRole() == UserRole.DIRECTOR) {
            return requested;
        }
        return requested.stream()
                .filter(c -> !PermissionCodes.USER_MANAGE.equals(c) && !PermissionCodes.ASSIGN_AGENT.equals(c))
                .collect(Collectors.toSet());
    }
}
