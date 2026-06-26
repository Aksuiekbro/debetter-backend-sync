package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.in.JudgeGetParams;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.repository.specification.tournament.JudgeSpecification;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@RequiredArgsConstructor
@Service
public class JudgeService {
    private final EntityManager entityManager;

    private final JudgeRepository judgeRepository;
    private final JudgeMapper judgeMapper;

    private final TournamentRepository tournamentRepository;

    @Transactional(readOnly = true)
    public Page<Judge> getJudges(Long tournamentId, JudgeGetParams params, Pageable pageable) {
        //TODO: validate tournament id exists
        Specification<Judge> spec = JudgeSpecification.filterBy(tournamentId, params, entityManager);
        return judgeRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Judge getJudgeByTournamentIdAndId(Long tournamentId, Long id) {
        return judgeRepository.findByTournamentIdAndId(tournamentId, id)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found"));
    }

    @Transactional
    public Judge addJudgeToTournament(JudgeFormDto judgeFormDto, Long tournamentId) {
        if (judgeFormDto.fullName() == null || judgeFormDto.fullName().isBlank()) {
            throw new IllegalArgumentException("Judge full name is required");
        }

        Tournament tournament = tournamentRepository.getReferenceById(tournamentId);

        Judge judge = judgeMapper.toJudge(judgeFormDto);

        judge.setTournament(tournament);
        if (judgeFormDto.checkedIn() == null) judge.setCheckedIn(false);
        judge.setTimesJudged(0);

        return judgeRepository.save(judge);
    }

    @Transactional
    public Judge updateJudge(JudgeFormDto judgeFormDto, Long tournamentId, Long judgeId) {
        Judge judge = judgeRepository.findByTournamentIdAndId(tournamentId, judgeId)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found"));

        judgeMapper.updateJudge(judgeFormDto, judge);

        return judgeRepository.save(judge);
    }

    @Transactional
    public void removeJudgeFromTournament(Long judgeId, Long tournamentId) {
        Judge judge = judgeRepository.findByTournamentIdAndId(tournamentId, judgeId)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found"));

        judgeRepository.deleteById(judgeId);
    }
}
