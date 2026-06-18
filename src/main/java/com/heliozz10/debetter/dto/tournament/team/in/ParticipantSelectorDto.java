package com.heliozz10.debetter.dto.tournament.team.in;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ParticipantSelectorDto(
        @Positive Long id,
        @Size(min = 1, max = 20) String username
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ParticipantSelectorDto fromJson(Object value) {
        if (value instanceof String username) {
            return new ParticipantSelectorDto(null, username);
        }

        if (value instanceof Map<?, ?> selector) {
            return new ParticipantSelectorDto(toLong(selector.get("id")), toString(selector.get("username")));
        }

        throw new IllegalArgumentException("Invalid participant selector");
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return null;
    }

    private static String toString(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }
}
