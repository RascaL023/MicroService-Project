package com.rascal.user_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BatchRequest(
    @NotNull(message = "Batch ID must be filled")
    @Min(value = 1, message = "Invalid batch ID")
    Integer id,

    @Size(min = 2, max = 50, message = "Batch name length is around 2 to 50 characters")
    String name
) { }
