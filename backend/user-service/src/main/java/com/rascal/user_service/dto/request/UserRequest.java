package com.rascal.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Name must be filled")
    @Size(min = 3, message = "Name length is around 3 to 50 characters", max = 50)
    String name,

    @NotBlank(message = "Email must be filled")
    @Email(message = "Invalid email")
    @Size(max = 254, message = "Email too long")
    String email,
    
    @NotNull(message = "Batch must be filled")
    @Min(value = 1, message = "Invalid batch")
    Integer batch,

    @NotBlank(message = "Major must be filled")
    String major,
    
    @NotNull(message = "Gender must be filled")
    Character gender
) { }
