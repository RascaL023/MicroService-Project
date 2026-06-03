package com.rascal.user_service.dto.request;

import jakarta.validation.constraints.Size;

public record BatchPatchRequest(
    @Size(min = 2, max = 50, message = "Batch name length is around 2 to 50 characters")
    String name
) {
    public boolean isEmptyPatch() {
        return name == null;
    }
}
