package com.heliozz10.debetter.dto.tournament.in;

import org.springframework.web.multipart.MultipartFile;

public record ScheduleFormDto(
    String name,
    String description,
    MultipartFile image
) {
}
