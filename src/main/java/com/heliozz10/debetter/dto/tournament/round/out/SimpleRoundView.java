package com.heliozz10.debetter.dto.tournament.round.out;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import lombok.Data;

@Data
public class SimpleRoundView {
    private Long id;
    private String name;
    private DebateFormat customFormat;
    private Integer roundNumber;
}
