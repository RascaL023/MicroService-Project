package com.rascal.user_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.user_service.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
    Page<User> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByEmailAndDeletedAtIsNull(String email);
    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);
    
}
