package com.heliozz10.debetter.dto.util.request.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParticipantInvitationDto (
        @NotNull String inviteeUsername,
        @NotNull @Positive Long teamId
) {}
