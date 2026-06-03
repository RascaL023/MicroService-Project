package com.rascal.user_service.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rascal.user_service.dto.mapper.UserMapper;
import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.User;
import com.rascal.user_service.event.UserEventPublisher;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class UserService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BANNED = "BANNED";

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final UserEventPublisher eventPublisher;

    public UserService(
        UserRepository userRepository,
        BatchRepository batchRepository,
        UserEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
    }


    @Transactional(readOnly = true)
    public Page<User> getAllPaged(String name, Pageable pageable) {
        return getAllPaged(name, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<User> getAllPaged(String name, Integer batchId, Pageable pageable) {
        String normalizedName = name == null ? "" : normalizeSearchName(name);

        return userRepository.searchActiveUsers(normalizedName, batchId, pageable);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
    
    @Transactional(readOnly = true)
    public Boolean existByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(normalizeEmail(email));
    }


    public User create(UserRequest request) {
        String email = normalizeEmail(request.email());

        if (existByEmail(email))
            throw new ConflictException("Email already exist");

        User user = UserMapper.toEntity(request);
        user.setName(normalizeName(request.name()));
        user.setEmail(email);
        user.setGender(normalizeGender(request.gender()));
        user.setBatch(getActiveBatch(request.batch()));
        user.setCreatedAt(LocalDateTime.now());
        user.setStatus(STATUS_ACTIVE);

        User saved = userRepository.save(user);
        eventPublisher.userCreated(saved, request.roleIds());

        return saved;
    }


    public User patch(Long id, UserPatchRequest request) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("Invalid patch");

        User user = getById(id);
        String oldEmail = user.getEmail();
        String oldStatus = user.getStatus();

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(email, id))
                throw new ConflictException("Email already exist");
            user.setEmail(normalizeEmail(email));
        }

        if (request.batch() != null) user.setBatch(getActiveBatch(request.batch()));
        if (request.name() != null) user.setName(normalizeName(request.name()));
        if (request.gender() != null) user.setGender(normalizeGender(request.gender()));
        if (request.status() != null) user.setStatus(normalizeStatus(request.status()));
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        if (request.email() != null && !oldEmail.equals(saved.getEmail())) {
            eventPublisher.userEmailUpdated(saved, oldEmail);
        }
        if (request.status() != null && !saved.getStatus().equals(oldStatus)) {
            eventPublisher.userStatusUpdated(saved);
        }
        if (request.roleIds() != null) {
            eventPublisher.userRolesUpdated(saved, request.roleIds());
        }

        return saved;
    }


    public void deleteById(Long id) {
        User user = getById(id);
        user.setDeletedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        eventPublisher.userDeleted(saved);
    }



    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Name must be filled");

        return normalized;
    }

    private String normalizeSearchName(String name) {
        if (name == null || name.isBlank())
            return null;

        return name.trim();
    }

    private String normalizeEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank())
            throw new BadRequestException("Email must be filled");

        return normalized;
    }

    private Character normalizeGender(Character gender) {
        char normalized = Character.toUpperCase(gender);
        if (normalized != 'L' && normalized != 'P')
            throw new BadRequestException("Gender must be L or P");

        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals(STATUS_ACTIVE) && !normalized.equals(STATUS_BANNED))
            throw new BadRequestException("Status must be ACTIVE or BANNED");

        return normalized;
    }

    private Batch getActiveBatch(Integer batchId) {
        return batchRepository.findByIdAndDeletedAtIsNull(batchId)
            .orElseThrow(() -> new NotFoundException("Batch not found"));
    }

}
