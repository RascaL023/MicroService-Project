package com.rascal.user_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rascal.user_service.entity.Batch;

public interface BatchRepository extends JpaRepository<Batch, Integer> {

    Optional<Batch> findByIdAndDeletedAtIsNull(Integer id);

    Page<Batch> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Batch> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);

    boolean existsByIdAndDeletedAtIsNull(Integer id);
    long countByDeletedAtIsNull();

}
