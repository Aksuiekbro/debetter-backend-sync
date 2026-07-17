package com.heliozz10.debetter.dto.tournament.announcement.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AnnouncementFormDto(
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 120) String title,
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 5000) String content,
        List<String> tags
) {
}
