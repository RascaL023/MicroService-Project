package com.rascal.user_service.dto.mapper;

import com.rascal.user_service.dto.request.UserPatchRequest;
import com.rascal.user_service.dto.request.UserRequest;
import com.rascal.user_service.dto.response.UserResponse;
import com.rascal.user_service.entity.User;

public class UserMapper {

    public static UserResponse toResponse(User user) {
        UserResponse response = new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(), 
            user.getBatch(),
            user.getGender() == 'L' ? 
                "Laki - laki" : "Perempuan",
            user.getIsBanned()
        );

        return response;
    }

    public static User toEntity(UserRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setBatch(request.batch());
        user.setGender(request.gender());

        return user;
    }

    public static User patch(User user, UserPatchRequest request) {
        if (request.isBanned() != null) user.setIsBanned(request.isBanned());
        if (request.batch() != null) user.setBatch(request.batch());

        return user;
    }
    
}
