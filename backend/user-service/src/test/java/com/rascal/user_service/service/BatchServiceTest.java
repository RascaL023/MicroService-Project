package com.rascal.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.rascal.user_service.dto.request.BatchPatchRequest;
import com.rascal.user_service.dto.request.BatchRequest;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.User;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
class BatchServiceTest {

    @Autowired
    private BatchService batchService;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        batchRepository.deleteAll();
    }

    @Test
    void createNormalizesInputBeforeSaving() {
        Batch created = batchService.create(new BatchRequest(23, "  Batch 23  "));

        assertThat(created.getId()).isEqualTo(23);
        assertThat(created.getName()).isEqualTo("Batch 23");
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void createRejectsDuplicateActiveBatchId() {
        batchService.create(new BatchRequest(23, "Batch 23"));

        assertThatThrownBy(() -> batchService.create(new BatchRequest(23, "Batch 23 Again")))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void getAllPagedDoesNotReturnSoftDeletedBatches() {
        Batch visible = batchService.create(new BatchRequest(23, "Batch 23"));
        Batch deleted = batchService.create(new BatchRequest(24, "Batch 24"));

        batchService.deleteById(deleted.getId());

        assertThat(batchService.getAllPaged(null, PageRequest.of(0, 10)).getContent())
            .extracting(Batch::getId)
            .containsExactly(visible.getId());
    }

    @Test
    void countActiveUsersByBatchIdsGroupsCountsInOneLookup() {
        Batch batch23 = batchService.create(new BatchRequest(23, "Batch 23"));
        Batch batch24 = batchService.create(new BatchRequest(24, "Batch 24"));
        Batch batch25 = batchService.create(new BatchRequest(25, "Batch 25"));
        userRepository.save(user(batch23, "dimas@example.com", null));
        userRepository.save(user(batch23, "budi@example.com", null));
        userRepository.save(user(batch23, "deleted@example.com", LocalDateTime.now()));
        userRepository.save(user(batch24, "sari@example.com", null));

        Map<Integer, Long> counts = batchService.countActiveUsersByBatchIds(
            java.util.List.of(batch23.getId(), batch24.getId(), batch25.getId())
        );

        assertThat(counts)
            .containsEntry(23, 2L)
            .containsEntry(24, 1L)
            .doesNotContainKey(25);
    }

    @Test
    void patchCanUpdateName() {
        Batch created = batchService.create(new BatchRequest(23, "Batch 23"));

        Batch patched = batchService.patch(created.getId(), new BatchPatchRequest("  Divisi 23  "));

        assertThat(patched.getName()).isEqualTo("Divisi 23");
        assertThat(patched.getUpdatedAt()).isNotNull();
    }

    @Test
    void patchRejectsEmptyPatch() {
        Batch created = batchService.create(new BatchRequest(23, "Batch 23"));

        assertThatThrownBy(() -> batchService.patch(created.getId(), new BatchPatchRequest(null)))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteByIdRejectsBatchWithActiveUsers() {
        Batch batch = batchService.create(new BatchRequest(23, "Batch 23"));
        userRepository.save(user(batch, null));

        assertThatThrownBy(() -> batchService.deleteById(batch.getId()))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteByIdAllowsBatchWithOnlySoftDeletedUsers() {
        Batch batch = batchService.create(new BatchRequest(23, "Batch 23"));
        userRepository.save(user(batch, LocalDateTime.now()));

        batchService.deleteById(batch.getId());

        Batch stored = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(stored.getDeletedAt()).isNotNull();
    }

    @Test
    void getByIdIgnoresSoftDeletedBatch() {
        Batch batch = batchService.create(new BatchRequest(23, "Batch 23"));
        batchService.deleteById(batch.getId());

        assertThatThrownBy(() -> batchService.getById(batch.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    private User user(Batch batch, LocalDateTime deletedAt) {
        return user(batch, "dimas@example.com", deletedAt);
    }

    private User user(Batch batch, String email, LocalDateTime deletedAt) {
        User user = new User();
        user.setName("Dimas");
        user.setEmail(email);
        user.setGender('L');
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setDeletedAt(deletedAt);
        user.setBatch(batch);
        return user;
    }
}
