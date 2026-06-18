package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.content.tournament.TournamentLeague;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentGetParams (
        @Size(min = 1, max = 50) String searchName,
        @Size(min = 1, max = 50) String searchLocation,
        List<String> tags,
        LocalDateTime startDateFrom,
        LocalDateTime startDateTo,
        LocalDateTime registrationDeadlineFrom,
        LocalDateTime registrationDeadlineTo,
        TournamentLeague league,
        Boolean nonFull
) {}
