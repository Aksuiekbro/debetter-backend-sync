package com.heliozz10.debetter.dto.tournament.in;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleFormDtoValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowsOperationalScheduleText() {
        String name = "Л/д 1/4 schedule and room allocation update";
        String description = """
                Б.К. Ескі махаббат тот баспайды д.е.
                Л/д 1/4
                1. Асан м vs Ақниет х 505 каб Төреші Асыл х, Аяулым м, Жігер м
                2. Мухаммедали м vs Арман м 507 каб Төреші Данияр м
                3. Ерасыл м vs Айша х 509 каб Төреші БТ, Еркежан х, Бекош м
                4. Аяжан х vs Гүлжамал х 520 каб Төреші Алтынбек м
                """;

        Set<ConstraintViolation<ScheduleFormDto>> violations = validator.validate(new ScheduleFormDto(name, description));

        assertTrue(violations.isEmpty(), () -> "Expected schedule text to be valid, but got: " + violations);
    }
}
