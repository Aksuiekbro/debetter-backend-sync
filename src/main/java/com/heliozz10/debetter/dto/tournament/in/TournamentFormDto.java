package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public record TournamentFormDto(
        @NotNull(groups = {OnCreate.class}) @Size(min = 1, max = 120) String name,
        @Size(min = 1, max = 5000) String description,
        @NotNull(groups = {OnCreate.class}) @Future LocalDateTime startDate,
        @Future LocalDateTime endDate,
        @NotNull(groups = {OnCreate.class}) @Future LocalDateTime registrationDeadline,
        @Size(min = 1, max = 255) String location,
        @NotNull(groups = {OnCreate.class}) TournamentLeague league,
        @Positive Integer teamLimit,
        @NotNull(groups = {OnCreate.class}) DebateFormat preliminaryFormat,
        @NotNull(groups = {OnCreate.class}) DebateFormat teamEliminationFormat,
        @NotNull(groups = {OnCreate.class}) @Positive Integer preliminaryRoundCount,
        @NotNull(groups = {OnCreate.class}) @Positive Integer eliminationRoundCount,
        Boolean ldEnabled,
        @Positive Integer ldRoundCount
) { }
