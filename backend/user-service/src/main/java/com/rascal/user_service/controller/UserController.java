package com.rascal.user_service.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rascal.user_service.dto.mapper.UserMapper;
import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.dto.response.UserLookupResponse;
import com.rascal.user_service.dto.response.UserResponse;
import com.rascal.user_service.service.UserBulkImportService;
import com.rascal.user_service.service.UserService;

import id.rascal.response_kit.util.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserBulkImportService userBulkImportService;

    public UserController(
        UserService userService,
        UserBulkImportService userBulkImportService
    ) {
        this.userService = userService;
        this.userBulkImportService = userBulkImportService;
    }


    @GetMapping
    @PreAuthorize("hasAnyAuthority('user.*', 'user.read')")
    public ResponseEntity<?> getAllPaged(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer batch,
        Pageable pageable
    ) {
        Page<UserResponse> users = userService.getAllPaged(name, batch, pageable)
            .map(UserMapper::toResponse);
        
        return ApiResponse.paged(
            HttpStatus.OK, 
            users
        );
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyAuthority('user.*', 'user.lookup')")
    public ResponseEntity<?> lookupByIds(@RequestParam Collection<Long> ids) {
        List<UserLookupResponse> responses = userService.lookupByIds(ids)
            .stream().map(UserMapper::toLookupResponse).toList();

        return ApiResponse.success(HttpStatus.OK, responses);
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAuthority('user.*')")
    public ResponseEntity<?> getDashboardSummary() {
        return ApiResponse.success(
            HttpStatus.OK,
            userService.getDashboardSummary()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user.*', 'user.read')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ApiResponse.success(
            HttpStatus.OK, 
            UserMapper.toResponse(userService.getById(id))
        );
    }


    @PostMapping
    @PreAuthorize("hasAnyAuthority('user.*', 'user.create')")
    public ResponseEntity<?> create(
        @Valid @RequestBody UserRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED, 
            UserMapper.toResponse(userService.create(request))
        );
    }

    @PostMapping(value = "/bulk/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('user.*', 'user.create')")
    public ResponseEntity<?> bulkCreateByExcel(
        @RequestParam Integer batch,
        @RequestParam MultipartFile file
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            userBulkImportService.importExcel(batch, file)
        );
    }


    @PatchMapping("/{id}")
    @PreAuthorize("""
        hasAuthority('user.*') or (
            #id == authentication.name and #request.batch == null and #request.gender == null
        )
    """
    ) public ResponseEntity<?> patchById(
        @PathVariable String id,
        @Valid @RequestBody UserPatchRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK, 
            UserMapper.toResponse(
                userService.patch(Long.parseLong(id), request)
            )
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user.*')")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
