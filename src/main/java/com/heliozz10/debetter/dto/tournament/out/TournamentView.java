package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.dto.tag.out.TagView;
import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TournamentView {
    private Long id;
    private String name;
    private String description;
    private UrlView imageUrl;
    private TournamentLeague league;
    private DebateFormat preliminaryFormat;
    private DebateFormat teamEliminationFormat;
    private List<TagView> tags;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationDeadline;
    private String location;
    private Integer teamLimit;
    private Boolean started;
    private Boolean finished;
    private Boolean disabled;
}
