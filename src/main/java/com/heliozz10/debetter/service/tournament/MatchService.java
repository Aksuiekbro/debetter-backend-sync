package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchUpdateDto;
import com.heliozz10.debetter.dto.tournament.match.in.ParticipantScoreDto;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final JudgeRepository judgeRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
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

    @Transactional
    public Match updateMatch(
            Long tournamentId,
            Long roundGroupId,
            Long roundId,
            Long matchId,
            MatchUpdateDto dto
    ) {
        Match match = matchRepository.findByTournamentRoundGroupRoundAndId(tournamentId, roundGroupId, roundId, matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found"));

        if(Boolean.TRUE.equals(match.getCompleted())) {
            throw new IllegalStateException("Cannot edit a completed match");
        }

        if(dto.hasLocation()) {
            match.setLocation(normalizeLocation(dto.getLocation()));
        }

        if(dto.hasStartTime()) {
            match.setStartTime(dto.getStartTime());
        }

        if(dto.hasJudgeId()) {
            match.setJudge(resolveJudge(tournamentId, dto.getJudgeId()));
        }

        if(dto.hasTeam1Id()) {
            match.setTeam1(resolveTeam(tournamentId, dto.getTeam1Id()));
        }

        if(dto.hasTeam2Id()) {
            match.setTeam2(resolveTeam(tournamentId, dto.getTeam2Id()));
        }

        if(dto.hasTeam3Id()) {
            match.setTeam3(resolveTeam(tournamentId, dto.getTeam3Id()));
        }

        if(dto.hasTeam4Id()) {
            match.setTeam4(resolveTeam(tournamentId, dto.getTeam4Id()));
        }

        if(dto.hasDebater1Id()) {
            match.setDebater1(resolveDebater(tournamentId, dto.getDebater1Id()));
        }

        if(dto.hasDebater2Id()) {
            match.setDebater2(resolveDebater(tournamentId, dto.getDebater2Id()));
        }

        assertUniqueTeamSlots(match);
        assertUniqueDebaterSlots(match);

        return matchRepository.save(match);
    }

    private String normalizeLocation(String location) {
        if(location == null) {
            return null;
        }

        String trimmed = location.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Judge resolveJudge(Long tournamentId, Long judgeId) {
        if(judgeId == null) {
            return null;
        }

        return judgeRepository.findByTournamentIdAndId(tournamentId, judgeId)
                .orElseThrow(() -> new EntityNotFoundException("Judge not found in tournament"));
    }

    private Team resolveTeam(Long tournamentId, Long teamId) {
        if(teamId == null) {
            return null;
        }

        return teamRepository.findByTournamentIdAndId(tournamentId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found in tournament"));
    }

    private TournamentParticipant resolveDebater(Long tournamentId, Long debaterId) {
        if(debaterId == null) {
            return null;
        }

        return tournamentParticipantRepository.findByTournamentIdAndId(tournamentId, debaterId)
                .orElseThrow(() -> new EntityNotFoundException("Debater not found in tournament"));
    }

    private void assertUniqueTeamSlots(Match match) {
        List<Long> teamIds = Stream.of(match.getTeam1(), match.getTeam2(), match.getTeam3(), match.getTeam4())
                .filter(Objects::nonNull)
                .map(Team::getId)
                .filter(Objects::nonNull)
                .toList();

        if(teamIds.stream().distinct().count() != teamIds.size()) {
            throw new IllegalArgumentException("A team cannot occupy multiple slots in the same match");
        }
    }

    private void assertUniqueDebaterSlots(Match match) {
        List<Long> debaterIds = Stream.of(match.getDebater1(), match.getDebater2())
                .filter(Objects::nonNull)
                .map(TournamentParticipant::getId)
                .filter(Objects::nonNull)
                .toList();

        if(debaterIds.stream().distinct().count() != debaterIds.size()) {
            throw new IllegalArgumentException("A debater cannot occupy multiple slots in the same match");
        }
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
                Integer score = null;
                Boolean won = null;
                if (result.teamResults() != null && i < result.teamResults().size()) {
                    var teamResult = result.teamResults().get(i);
                    List<ParticipantScoreDto> ps = teamResult.participantScores();
                    score = ps.stream()
                            .map(ParticipantScoreDto::score)
                            .filter(Objects::nonNull)
                            .reduce(0, Integer::sum);
                    won = teamResult.won();
                }
                // Positional mapping (teamResults[i] -> team{i+1}), matching the frontend payload order.
                objectNode.put("team" + (i + 1) + "score", score);
                objectNode.put("team" + (i + 1) + "won", won);
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
