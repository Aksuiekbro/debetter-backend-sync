package com.heliozz10.debetter.dto.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record NewsDto (
    @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 100) String title,
    @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 1000) String content,
    @Size(max = 20) List<@Size(min = 1, max = 20) String> tags
) {}
