package com.heliozz10.debetter.dto.tournament.announcement.in;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncementFormDtoValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void allowsOperationalAnnouncementText() {
        String title = "Қайырлы таң құрметті пікірсайысшылар! Тіркеу ашық";
        String content = """
                Б.К. Ескі махаббат тот баспайды д.е.
                Л/д 1/4
                1. Асан м vs Ақниет х 505 каб Төреші Асыл х, Аяулым м, Жігер м
                2. Мухаммедали м vs Арман м 507 каб Төреші Данияр м
                3. Ерасыл м vs Айша х 509 каб Төреші БТ, Еркежан х, Бекош м
                4. Аяжан х vs Гүлжамал х 520 каб Төреші Алтынбек м
                """;

        Set<ConstraintViolation<AnnouncementFormDto>> violations = validator.validate(new AnnouncementFormDto(title, content, null));

        assertTrue(violations.isEmpty(), () -> "Expected the announcement to be valid, but got: " + violations);
    }
}
