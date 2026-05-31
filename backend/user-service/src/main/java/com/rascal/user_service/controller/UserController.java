package com.rascal.user_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.user_service.dto.mapper.UserMapper;
import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.dto.response.UserResponse;
import com.rascal.user_service.service.UserService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllPaged(name, pageable)
            .map(UserMapper::toResponse);
        
        return ApiResponse.paged(
            HttpStatus.OK, 
            users
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            UserMapper.toResponse(userService.getById(id))
        );
    }

    @GetMapping("/exists")
    public ResponseEntity<?> existsBy(@RequestParam("email") String email) {
        return ApiResponse.success(
            HttpStatus.OK, 
            userService.existByEmail(email)
        );
    }


    @PostMapping
    public ResponseEntity<?> create(
        @Valid @RequestBody UserRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED, 
            UserMapper.toResponse(userService.create(request))
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchById(
        @PathVariable Long id,
        @Valid @RequestBody UserPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            UserMapper.toResponse(userService.patch(id, request))
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
