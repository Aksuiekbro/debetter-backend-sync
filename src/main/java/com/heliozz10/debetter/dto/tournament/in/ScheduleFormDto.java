package com.heliozz10.debetter.dto.tournament.in;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record ScheduleFormDto(
    @Size(min = 1, max = 120) String name,
    @Size(min = 1, max = 5000) String description
) {
}
