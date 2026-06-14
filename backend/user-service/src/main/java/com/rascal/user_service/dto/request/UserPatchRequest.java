package com.rascal.user_service.dto.request;

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

    Character gender
) { 
    public boolean isEmptyPatch() {
        return 
            this.name() == null && this.email() == null && 
            this.batch() == null && this.gender() == null;
    }
}
