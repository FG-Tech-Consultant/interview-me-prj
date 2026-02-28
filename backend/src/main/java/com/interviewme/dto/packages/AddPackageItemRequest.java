package com.interviewme.dto.packages;

import jakarta.validation.constraints.NotNull;

public record AddPackageItemRequest(
    @NotNull(message = "Item ID is required")
    Long itemId,

    Integer displayOrder
) {
}
