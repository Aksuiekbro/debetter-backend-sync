package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.content.tournament.TournamentLeague;

import java.time.LocalDateTime;
import java.util.List;

public record TournamentGetParams (
        String searchName,
        String searchLocation,
        List<String> tags,
        LocalDateTime startDateFrom,
        LocalDateTime startDateTo,
        LocalDateTime registrationDeadlineFrom,
        LocalDateTime registrationDeadlineTo,
        TournamentLeague league,
        Boolean nonFull
) {}
