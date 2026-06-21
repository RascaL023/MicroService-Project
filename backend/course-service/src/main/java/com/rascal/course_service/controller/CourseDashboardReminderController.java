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
@RequestMapping("/api/courses/dashboard-reminders")
public class CourseDashboardReminderController {

    private final CourseDashboardService dashboardService;

    public CourseDashboardReminderController(CourseDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('course.*', 'group.*', 'group.read', 'enrollment.*')")
    public ResponseEntity<?> getReminders() {
        return ApiResponse.success(
            HttpStatus.OK,
            dashboardService.getReminders()
        );
    }
}
