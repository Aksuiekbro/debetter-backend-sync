package com.heliozz10.debetter.dto.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewsDto (
    @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 100) String title,
    @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 1000) String content,
    @Size(max = 20) List<@Size(min = 1, max = 20) String> tags,
    @Null(groups = {OnCreate.class}) @Size(max = 10) List<@NotNull Long> retainedImageIds,
    @Null(groups = {OnCreate.class}) @Size(max = 10) List<@NotNull @Min(0) Integer> newImagePositions
) {
    public NewsDto(String title, String content, List<String> tags) {
        this(title, content, tags, null, null);
    }

    public NewsDto(String title, String content, List<String> tags, List<Long> retainedImageIds) {
        this(title, content, tags, retainedImageIds, null);
    }
}
