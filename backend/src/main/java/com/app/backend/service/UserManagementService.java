package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.admin.ManagedUserDetailResponse;
import com.app.backend.dto.admin.ManagedUserListItemResponse;
import com.app.backend.dto.admin.UserCreateRequest;
import com.app.backend.dto.admin.UserPermissionsRequest;
import com.app.backend.dto.admin.UserUpdateRequest;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

public interface UserManagementService {

    PageResponse<ManagedUserListItemResponse> list(UserPrincipal actor, Pageable pageable);

    ManagedUserDetailResponse get(UserPrincipal actor, Long id);

    ManagedUserDetailResponse create(UserPrincipal actor, UserCreateRequest request);

    ManagedUserDetailResponse update(UserPrincipal actor, Long id, UserUpdateRequest request);

    void deactivate(UserPrincipal actor, Long id);

    void replacePermissions(UserPrincipal actor, Long id, UserPermissionsRequest request);
}
