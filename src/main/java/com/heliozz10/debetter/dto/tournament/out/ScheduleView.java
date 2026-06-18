package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

@Data
public class ScheduleView {
    private Long id;
    private String name;
    private String description;
    private UrlView imageUrl;
}
