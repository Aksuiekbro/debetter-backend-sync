package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.ParticipantScoreDto;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final RoundRepository roundRepository;
    private final TournamentSecurity tournamentSecurity;

    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<Match> getMatchesByRoundId(Long roundId, Pageable pageable) {
        return matchRepository.findByRoundId(roundId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Match> getVisibleMatchesByRoundId(
            Long tournamentId,
            Long roundGroupId,
            Long roundId,
            Authentication authentication,
            Pageable pageable
    ) {
        Round round = roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(tournamentId, roundGroupId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        if(Boolean.TRUE.equals(round.getMatchesArePublic()) || hasTournamentViewPermission(authentication, tournamentId)) {
            return matchRepository.findByRoundId(roundId, pageable);
        }

        return Page.empty(pageable);
    }

    private boolean hasTournamentViewPermission(Authentication authentication, Long tournamentId) {
        if(authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }

        return tournamentSecurity.hasViewPermission(user, tournamentId);
    }

    //TODO: fix this, this doesnt set the scores for the teams and debaters, it only sets the scores for the matches. Already done but keeping the todo
    @Transactional
    public void submitMatchResults(Long tournamentId, Collection<MatchResultDto> results) {
        if(results == null || results.isEmpty()) {
            return;
        }

        List<Long> matchIds = results.stream()
                .map(MatchResultDto::matchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if(matchIds.isEmpty()) {
            return;
        }

        if(matchRepository.countMatchesInTournament(tournamentId, matchIds) != matchIds.size()) {
            throw new IllegalArgumentException("Some match results do not belong to this tournament.");
        }

        ArrayNode arrayNode = objectMapper.createArrayNode();

        results.forEach(result -> {
            ObjectNode objectNode = objectMapper.createObjectNode();

            objectNode.put("tournament_id", tournamentId);

            objectNode.put("match_id", result.matchId());

            for (int i = 0; i < 4; i++) {
                String fieldName = "team" + (i + 1) + "score";
                Integer score = null;
                if (result.teamResults() != null && i < result.teamResults().size()) {
                    List<ParticipantScoreDto> ps = result.teamResults().get(i).participantScores();
                    score = ps.stream()
                            .map(ParticipantScoreDto::score)
                            .filter(Objects::nonNull)
                            .reduce(0, Integer::sum);
                }
                objectNode.put(fieldName, score);
            }

            for (int i = 0; i < 2; i++) {
                String fieldName = "debater" + (i + 1) + "score";
                Integer score = null;
                if (result.participantScores() != null && i < result.participantScores().size()) {
                    score = result.participantScores().get(i).score();
                }
                objectNode.put(fieldName, score);
            }

            arrayNode.add(objectNode);
        });

        String json = arrayNode.toString();
        matchRepository.updateMatchScoresBulk(json);
    }
}
