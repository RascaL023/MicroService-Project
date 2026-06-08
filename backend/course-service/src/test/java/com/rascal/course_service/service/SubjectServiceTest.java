package com.rascal.course_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.rascal.course_service.dto.request.SubjectRequest;
import com.rascal.course_service.entity.Subject;
import com.rascal.course_service.repository.SubjectRepository;

import id.rascal.response_kit.exception.ConflictException;

@SpringBootTest
@ActiveProfiles("test")
class SubjectServiceTest {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectRepository subjectRepository;

    @BeforeEach
    void setUp() {
        subjectRepository.deleteAll();
    }

    @Test
    void createRejectsDuplicateNameFromDatabaseConstraint() {
        Subject created = subjectService.create(new SubjectRequest("Bahasa C"));

        assertThat(created.getId()).isNotNull();
        assertThatThrownBy(() -> subjectService.create(new SubjectRequest("Bahasa C")))
            .isInstanceOf(ConflictException.class);
    }
}
