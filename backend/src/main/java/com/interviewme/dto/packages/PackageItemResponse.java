package com.interviewme.dto.packages;

public record PackageItemResponse<T>(
    Long id,
    T item,
    Integer displayOrder
) {
}
