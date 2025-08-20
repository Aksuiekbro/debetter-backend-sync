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
        Tournament tournament = tournamentRepository.findById(judgeFormDto.tournamentId())
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        Judge judge = judgeMapper.toJudge(judgeFormDto);

        judge.setTournament(tournament);
        tournament.getJudges().add(judge);
        judge.setTimesJudged(0);

        return judgeRepository.save(judge);
    }

    public Judge updateJudge(JudgeFormDto judgeFormDto, Long tournamentId, Long judgeId) {
        Judge judge = judgeRepository.findById(judgeId)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found"));

        if(!Objects.equals(judge.getTournament().getId(), tournamentId)) {
            throw new IllegalArgumentException("Judge does not belong to this tournament");
        }

        judgeMapper.updateJudge(judgeFormDto, judge);

        return judgeRepository.save(judge);
    }

    @Transactional
    public void removeJudgeFromTournament(Long judgeId, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new EntityNotFoundException("Tournament not found"));

        tournament.getJudges().removeIf(j -> Objects.equals(j.getId(), judgeId));
    }
}
