package com.heliozz10.debetter.dto.tournament.team.in;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamFormDtoValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowsRealTeamAndClubNames() {
        TeamFormDto dto = new TeamFormDto(
                "KTL Kaprichoza Debate Team",
                "Kazakh Turkish Lyceum Debate Society",
                null,
                List.of()
        );

        Set<ConstraintViolation<TeamFormDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), () -> "Expected team registration to be valid, but got: " + violations);
    }
}
