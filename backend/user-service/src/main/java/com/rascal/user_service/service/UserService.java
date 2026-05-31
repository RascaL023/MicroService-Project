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
import com.rascal.user_service.entity.User;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Transactional(readOnly = true)
    public Page<User> getAllPaged(String name, Pageable pageable) {
        if (name == null || name.isBlank())
            return userRepository.findAllByDeletedAtIsNull(pageable);

        return userRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(name.trim(), pageable);
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
        user.setCreatedAt(LocalDateTime.now());
        user.setIsBanned(false);

        return userRepository.save(user);
    }


    public User patch(Long id, UserPatchRequest request) {
        if (request.isEmptyPatch()) 
            throw new BadRequestException("Invalid patch");

        User user = getById(id);

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(email, id))
                throw new ConflictException("Email already exist");
        }

        UserMapper.patch(user, request);

        if (request.name() != null) user.setName(normalizeName(request.name()));
        if (request.email() != null) user.setEmail(normalizeEmail(request.email()));
        if (request.gender() != null) user.setGender(normalizeGender(request.gender()));
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }


    public void deleteById(Long id) {
        User user = getById(id);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank())
            throw new BadRequestException("Name must be filled");

        return normalized;
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

}
