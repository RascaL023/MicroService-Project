package com.rascal.course_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rascal.course_service.dto.request.SubjectMaterialRequest;
import com.rascal.course_service.dto.request.SubjectRequest;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.entity.SubjectMaterial;
import com.rascal.course_service.repository.SubjectMaterialRepository;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.ConflictException;

@SpringBootTest
@ActiveProfiles("test")
class SubjectMaterialServiceTest {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectMaterialService subjectMaterialService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectMaterialRepository subjectMaterialRepository;

    @BeforeEach
    void setUp() {
        subjectMaterialRepository.deleteAll();
        subjectRepository.deleteAll();
    }

    @Test
    void createRejectsDuplicateMeetingNumberInSameSubjectOnly() {
        Subject java = subjectService.create(new SubjectRequest("Java Fundamental"));
        Subject c = subjectService.create(new SubjectRequest("Bahasa C"));

        SubjectMaterial created = subjectMaterialService.create(new SubjectMaterialRequest(
            java.getId(),
            1,
            "Pengenalan environment",
            "Setup tools dan struktur program dasar"
        ));

        assertThat(created.getId()).isNotNull();
        assertThatThrownBy(() -> subjectMaterialService.create(new SubjectMaterialRequest(
            java.getId(),
            1,
            "Tipe data",
            null
        ))).isInstanceOf(ConflictException.class);

        SubjectMaterial sameMeetingInOtherSubject = subjectMaterialService.create(new SubjectMaterialRequest(
            c.getId(),
            1,
            "Pengenalan pointer",
            null
        ));

        assertThat(sameMeetingInOtherSubject.getId()).isNotNull();
    }
}
