package com.rascal.course_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.service.CourseDashboardService;

import id.rascal.response_kit.util.ApiResponse;

@RestController
@RequestMapping("/api/courses/dashboard-summary")
public class CourseDashboardController {

    private final CourseDashboardService dashboardService;

    public CourseDashboardController(CourseDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('group.*', 'group.read')")
    public ResponseEntity<?> getSummary() {
        return ApiResponse.success(
            HttpStatus.OK,
            dashboardService.getSummary()
        );
    }
}
