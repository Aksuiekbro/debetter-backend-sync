package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

@Data
public class TournamentMapView {
    private Long id;
    private String title;
    private String description;
    private UrlView imageUrl;
}
