package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private JudgeRepository judgeRepository;

    @Mock
    private JudgeMapper judgeMapper;

    @Mock
    private TournamentRepository tournamentRepository;

    private JudgeService judgeService;

    @BeforeEach
    void setUp() {
        judgeService = new JudgeService(entityManager, judgeRepository, judgeMapper, tournamentRepository);
    }

    @Test
    void addJudgeRejectsBlankFullNameBeforeSaving() {
        JudgeFormDto dto = new JudgeFormDto(" ", "+77010000000", "judge@example.com", null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> judgeService.addJudgeToTournament(dto, 53L)
        );

        assertEquals("Judge full name is required", exception.getMessage());
        verifyNoInteractions(tournamentRepository, judgeMapper, judgeRepository);
    }
}
