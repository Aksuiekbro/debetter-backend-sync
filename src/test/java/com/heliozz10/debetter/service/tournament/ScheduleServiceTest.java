package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Schedule;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.dto.tournament.in.ScheduleFormDto;
import com.heliozz10.debetter.mapper.tournament.ScheduleMapper;
import com.heliozz10.debetter.repository.tournament.ScheduleRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {
    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private FileService fileService;

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(scheduleRepository, scheduleMapper, tournamentRepository, fileService);
    }

    @Test
    void addScheduleAllowsMissingImage() {
        ScheduleFormDto dto = new ScheduleFormDto("Round 1", "Room allocations will be posted soon.");
        Tournament tournament = new Tournament();
        Schedule schedule = new Schedule();

        when(tournamentRepository.getReferenceById(42L)).thenReturn(tournament);
        when(scheduleMapper.toSchedule(dto)).thenReturn(schedule);
        when(scheduleRepository.save(schedule)).thenReturn(schedule);

        Schedule saved = scheduleService.addScheduleToTournament(dto, null, 42L);

        assertSame(schedule, saved);
        assertSame(tournament, schedule.getTournament());
        assertNull(schedule.getImageUrl());
        verify(fileService, never()).uploadImage(any(), any(), any());
    }
}
