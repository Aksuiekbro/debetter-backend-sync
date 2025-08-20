package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public record TournamentFormDto(
    String name,
    String description,
    MultipartFile image,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime registrationDeadline,
    String location,
    TournamentLeague league,
    Integer teamLimit,
    DebateFormat preliminaryFormat,
    DebateFormat teamEliminationFormat,
    Integer preliminaryRoundCount,
    Integer eliminationRoundCount
) { }
