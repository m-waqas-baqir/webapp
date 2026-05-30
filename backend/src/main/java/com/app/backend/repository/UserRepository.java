package com.app.backend.repository;

import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByNameIgnoreCaseAndRole(String name, UserRole role);

    List<User> findByRoleOrderByNameAsc(UserRole role);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);
}
