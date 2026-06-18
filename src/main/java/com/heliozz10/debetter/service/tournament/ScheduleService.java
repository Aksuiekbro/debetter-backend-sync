package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Schedule;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.dto.tournament.in.ScheduleFormDto;
import com.heliozz10.debetter.mapper.tournament.ScheduleMapper;
import com.heliozz10.debetter.repository.tournament.ScheduleRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.service.util.media.FileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    private final TournamentRepository tournamentRepository;

    private final FileService fileService;

    @Transactional(readOnly = true)
    public List<Schedule> getSchedulesByTournamentId(Long tournamentId) {
        return scheduleRepository.findByTournamentId(tournamentId);
    }

    @Transactional(readOnly = true)
    public Schedule getScheduleByTournamentIdAndId(Long tournamentId, Long id) {
        return scheduleRepository.findByTournamentIdAndId(tournamentId, id)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
    }

    @Transactional
    public Schedule addScheduleToTournament(ScheduleFormDto scheduleFormDto, MultipartFile image, Long tournamentId) {
        Tournament tournament = tournamentRepository.getReferenceById(tournamentId);

        Schedule schedule = scheduleMapper.toSchedule(scheduleFormDto);

        schedule.setTournament(tournament);

        if (image != null) {
            Url url = fileService.uploadImage(image, "schedules", UUID.randomUUID().toString());
            schedule.setImageUrl(url);
        }

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void removeScheduleFromTournament(Long scheduleId, Long tournamentId) {
        Schedule schedule = scheduleRepository.findByTournamentIdAndId(tournamentId, scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));

        fileService.deleteFile(schedule.getImageUrl());

        scheduleRepository.deleteById(scheduleId);
    }
}
