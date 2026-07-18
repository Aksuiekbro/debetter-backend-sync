package com.heliozz10.debetter.dto.tournament.in;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentFormDtoValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowsRealTournamentCopy() {
        TournamentFormDto dto = new TournamentFormDto(
                "Climate Tech or Climate Trap: Can Innovation Alone Save the Planet?",
                """
                        As the climate crisis accelerates, world leaders and corporations are betting on technological
                        solutions, from carbon capture to geoengineering. This tournament examines whether innovation
                        alone can solve climate change or whether deeper political and economic shifts are required.
                        """,
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(11),
                LocalDateTime.now().plusDays(9),
                "Almaty, Kazakhstan - Rixos Almaty Hotel",
                TournamentLeague.SCHOOL,
                32,
                DebateFormat.APF,
                DebateFormat.APF,
                3,
                5,
                true
        );

        Set<ConstraintViolation<TournamentFormDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), () -> "Expected tournament copy to be valid, but got: " + violations);
    }
}
