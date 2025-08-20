package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.util.media.Url;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class TournamentView extends SimpleTournamentView {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationDeadline;
    private String location;
    private Integer teamLimit;
}
