package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TournamentMapFormDto(
        @NotBlank(groups = OnCreate.class)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        @Size(max = 120)
        String title,
        @NotBlank(groups = OnCreate.class)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        @Size(max = 5000)
        String description
) {
}
