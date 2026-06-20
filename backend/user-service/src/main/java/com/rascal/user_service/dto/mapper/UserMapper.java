package com.rascal.user_service.dto.mapper;

import com.rascal.user_service.dto.response.UserDetailedResponse;
import com.rascal.user_service.dto.response.UserLookupResponse;
import com.rascal.user_service.dto.response.UserResponse;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.Major;
import com.rascal.user_service.entity.User;
import com.rascal.user_service.entity.UserStatus;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        Batch batch = user.getBatch();
        Major major = user.getMajor();
        String batchName = batch.getName() != null && !batch.getName().isBlank() ?
            batch.getId().toString() + " - " + batch.getName() : batch.getId().toString();

        return new UserResponse(
            user.getId(),
            user.getName(),
            major == null ? null : major.getId(),
            major == null ? null : major.getName(),
            batchName
        );

    }

    public static UserDetailedResponse toDetailedResponse(User user) {
        Batch batch = user.getBatch();
        Major major = user.getMajor();
        String batchName = batch.getName() != null && !batch.getName().isBlank() ?
            batch.getId().toString() + " - " + batch.getName() : batch.getId().toString();

        return new UserDetailedResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            batchName,
            major == null ? null : major.getId(),
            major == null ? null : major.getName(),
            user.getGender() == 'L' ?
                "Laki - laki" : "Perempuan",
            user.getStatus().name(),
            user.getGraduatedAt(),
            user.getCreatedAt()
        );
    }


    public static UserLookupResponse toLookupResponse(User user) {
        return new UserLookupResponse(
            user.getId(),
            user.getName(),
            user.getGender(),
            user.getBatch().getId()
        );
    }

    public static void toEntity(
        User user, String name, String email, 
        Character gender, Batch batch, Major major, UserStatus status
    ) {
        user.setName(name);
        user.setEmail(email);
        user.setGender(gender);
        user.setBatch(batch);
        user.setMajor(major);
        user.setStatus(status);
    }

}
