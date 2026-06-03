package com.rascal.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.User;
import com.rascal.user_service.repository.BatchRepository;
import com.rascal.user_service.repository.UserRepository;

import id.rascal.response_kit.exception.BadRequestException;
import id.rascal.response_kit.exception.ConflictException;
import id.rascal.response_kit.exception.NotFoundException;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BatchRepository batchRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        batchRepository.deleteAll();
        seedBatch(23);
        seedBatch(24);
    }

    @Test
    void createNormalizesInputBeforeSaving() {
        User created = userService.create(new UserRequest(
            "  Dimas  ",
            "  DIMAS@EXAMPLE.COM  ",
            23,
            'l',
            List.of(1L)
        ));

        assertThat(created.getName()).isEqualTo("Dimas");
        assertThat(created.getEmail()).isEqualTo("dimas@example.com");
        assertThat(created.getGender()).isEqualTo('L');
        assertThat(created.getBatch().getId()).isEqualTo(23);
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void patchCanMoveUserToAnotherBatch() {
        User created = createUser("Dimas", "dimas@example.com", 'L');

        User patched = userService.patch(created.getId(), new UserPatchRequest(
            null,
            null,
            null,
            24,
            null,
            null,
            null
        ));

        assertThat(patched.getBatch().getId()).isEqualTo(24);
    }

    @Test
    void patchCanUpdateStatus() {
        User created = createUser("Dimas", "dimas@example.com", 'L');

        User patched = userService.patch(created.getId(), new UserPatchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            "banned"
        ));

        assertThat(patched.getStatus()).isEqualTo("BANNED");
    }

    @Test
    void patchRejectsInvalidStatus() {
        User created = createUser("Dimas", "dimas@example.com", 'L');

        assertThatThrownBy(() -> userService.patch(created.getId(), new UserPatchRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            "LOCKED"
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsDuplicateEmail() {
        createUser("Dimas", "dimas@example.com", 'L');

        assertThatThrownBy(() -> createUser("Dimas Two", "DIMAS@example.com", 'L'))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsInvalidGender() {
        assertThatThrownBy(() -> createUser("Dimas", "dimas@example.com", 'X'))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getByIdIgnoresSoftDeletedUsers() {
        User created = createUser("Dimas", "dimas@example.com", 'L');

        userService.deleteById(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllPagedDoesNotReturnSoftDeletedUsers() {
        User visible = createUser("Dimas", "dimas@example.com", 'L');
        User deleted = createUser("Budi", "budi@example.com", 'L');

        userService.deleteById(deleted.getId());

        assertThat(userService.getAllPaged(null, PageRequest.of(0, 10)).getContent())
            .extracting(User::getId)
            .containsExactly(visible.getId());
    }

    @Test
    void getAllPagedCanFilterByBatch() {
        User batch23User = createUser("Dimas", "dimas@example.com", 'L', 23);
        createUser("Budi", "budi@example.com", 'L', 24);

        assertThat(userService.getAllPaged(null, 23, PageRequest.of(0, 10)).getContent())
            .extracting(User::getId)
            .containsExactly(batch23User.getId());
    }

    @Test
    void getAllPagedCanSearchNameInsideBatch() {
        User matched = createUser("Dimas Pratama", "dimas@example.com", 'L', 23);
        createUser("Dimas Saputra", "dimas24@example.com", 'L', 24);
        createUser("Budi Pratama", "budi@example.com", 'L', 23);

        assertThat(userService.getAllPaged("dimas", 23, PageRequest.of(0, 10)).getContent())
            .extracting(User::getId)
            .containsExactly(matched.getId());
    }

    @Test
    void getAllPagedReturnsEmptyPageForUnknownBatchFilter() {
        assertThat(userService.getAllPaged(null, 99, PageRequest.of(0, 10)).getContent())
            .isEmpty();
    }

    @Test
    void patchRejectsDuplicateEmailForAnotherUser() {
        User dimas = createUser("Dimas", "dimas@example.com", 'L');
        createUser("Budi", "budi@example.com", 'L');

        assertThatThrownBy(() -> userService.patch(dimas.getId(), new UserPatchRequest(
            null,
            "BUDI@example.com",
            null,
            null,
            null,
            null,
            null
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteByIdMarksDeletedAtInsteadOfDeletingRow() {
        User created = createUser("Dimas", "dimas@example.com", 'L');

        userService.deleteById(created.getId());

        User stored = userRepository.findById(created.getId()).orElseThrow();
        assertThat(stored.getDeletedAt()).isNotNull();
    }

    private User createUser(String name, String email, Character gender) {
        return createUser(name, email, gender, 23);
    }

    private User createUser(String name, String email, Character gender, Integer batch) {
        return userService.create(new UserRequest(
            name,
            email,
            batch,
            gender,
            List.of(1L)
        ));
    }

    private void seedBatch(Integer id) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setName("Batch " + id);
        batch.setCreatedAt(java.time.LocalDateTime.now());
        batchRepository.save(batch);
    }
}
