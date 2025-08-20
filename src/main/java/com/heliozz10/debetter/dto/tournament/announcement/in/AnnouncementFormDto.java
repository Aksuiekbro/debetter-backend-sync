package com.heliozz10.debetter.dto.tournament.announcement.in;

import org.springframework.web.multipart.MultipartFile;

public record AnnouncementFormDto(
    String title,
    String content,
    MultipartFile image
) {
}
