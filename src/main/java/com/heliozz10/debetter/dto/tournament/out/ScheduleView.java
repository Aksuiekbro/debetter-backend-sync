package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.content.util.media.Url;
import lombok.Data;

@Data
public class ScheduleView {
    private Long id;
    private String name;
    private String description;
    private Url imageUrl;
}
