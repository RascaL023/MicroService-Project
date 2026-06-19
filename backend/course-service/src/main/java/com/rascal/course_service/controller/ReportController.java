package com.rascal.course_service.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rascal.course_service.service.WeeklyGradeReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final WeeklyGradeReportService weeklyGradeReportService;

    public ReportController(WeeklyGradeReportService weeklyGradeReportService) {
        this.weeklyGradeReportService = weeklyGradeReportService;
    }

    @GetMapping("/weekly-grades")
    @PreAuthorize("hasAnyAuthority('course.*')")
    public ResponseEntity<byte[]> weeklyGrades(
        @RequestParam String academicYear,
        @RequestParam Long subjectId
    ) {
        WeeklyGradeReportService.ReportFile report = weeklyGradeReportService.generate(
            academicYear,
            subjectId
        );

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(report.filename())
                .build()
                .toString())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(report.content());
    }

}
