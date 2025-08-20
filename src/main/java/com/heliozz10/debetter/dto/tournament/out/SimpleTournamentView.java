package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tag.out.TagView;
import lombok.Data;

import java.util.List;

@Data
public class SimpleTournamentView {
    private Long id;
    private String name;
    private String description;
    private Url imageUrl;
    private TournamentLeague league;
    private List<TagView> tags;
}
