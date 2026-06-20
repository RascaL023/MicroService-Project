package com.rascal.user_service.dto.request;

import java.time.LocalDate;

import com.rascal.user_service.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UserPatchRequest(
    @Size(min = 3, message = "Name length is around 3 to 50 characters", max = 50)
    String name,

    @Email(message = "Invalid email")
    @Size(max = 254, message = "Email too long")
    String email,

    @Min(value = 1, message = "Batch invalid")
    Integer batch,

    String major,

    Character gender,

    LocalDate graduatedAt,

    UserStatus status
) { 
    public boolean isEmptyPatch() {
        return 
            this.name() == null && this.email() == null && 
            this.batch() == null && this.major() == null &&
            this.gender() == null && this.graduatedAt() == null &&
            this.status() == null;
    }
}
