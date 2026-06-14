package com.rascal.user_service.dto.mapper;

import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.dto.response.UserLookupResponse;
import com.rascal.user_service.dto.response.UserResponse;
import com.rascal.user_service.entity.Batch;
import com.rascal.user_service.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        Batch batch = user.getBatch();
        String batchName = batch.getName() != null && !batch.getName().isBlank() ?
            batch.getId().toString() + " - " + batch.getName() : batch.getId().toString();

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(), 
            batchName,
            user.getGender() == 'L' ? 
                "Laki - laki" : "Perempuan",
            user.getStatus()
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

    public static User toEntity(UserRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setGender(request.gender());

        return user;
    }

}
