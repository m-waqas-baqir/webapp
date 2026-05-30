package com.app.backend.repository;

import com.app.backend.entity.Permission;
import com.app.backend.entity.User;
import com.app.backend.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    @Query("select distinct p.code from UserPermission up join up.permission p where up.user.id = :userId")
    Set<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    long countByUser_Id(Long userId);

    @Modifying
    @Query("delete from UserPermission up where up.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserPermission> findAllByUser(User user);

    boolean existsByUserAndPermission(User user, Permission permission);

}
