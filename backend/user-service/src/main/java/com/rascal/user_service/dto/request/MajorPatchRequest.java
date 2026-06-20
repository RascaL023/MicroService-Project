package com.rascal.user_service.dto.request;

import jakarta.validation.constraints.Size;

public record MajorPatchRequest(
    @Size(max = 100, message = "Major name too long")
    String name
) {
    public boolean isEmptyPatch() {
        return name == null;
    }
}
