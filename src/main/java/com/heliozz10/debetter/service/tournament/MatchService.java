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
import java.util.stream.Collectors;
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
    public void submitMatchResults(Long tournamentId, Long roundGroupId, Long roundId, Collection<MatchResultDto> results) {
        if(results == null || results.isEmpty()) {
            return;
        }

        List<Long> allMatchIds = results.stream()
                .map(MatchResultDto::matchId)
                .filter(Objects::nonNull)
                .toList();

        List<Long> matchIds = allMatchIds.stream().distinct().toList();

        if(matchIds.isEmpty()) {
            return;
        }

        if(matchIds.size() != allMatchIds.size()) {
            throw new IllegalArgumentException("Duplicate match IDs in the submission.");
        }

        if(matchRepository.countMatchesInRound(tournamentId, roundGroupId, roundId, matchIds) != matchIds.size()) {
            throw new IllegalArgumentException("Some match results do not belong to the specified round.");
        }

        if(matchRepository.countCompletedMatches(matchIds) > 0) {
            throw new IllegalStateException("Cannot re-submit results for already completed matches.");
        }

        Map<Long, Match> matchesById = matchRepository.findAllById(matchIds).stream()
                .collect(Collectors.toMap(Match::getId, m -> m));

        ArrayNode arrayNode = objectMapper.createArrayNode();

        results.forEach(result -> {
            ObjectNode objectNode = objectMapper.createObjectNode();

            objectNode.put("tournament_id", tournamentId);
            objectNode.put("match_id", result.matchId());

            Match match = matchesById.get(result.matchId());
            Map<Long, Integer> teamSlotByTeamId = new HashMap<>();
            if (match != null) {
                if (match.getTeam1() != null) teamSlotByTeamId.put(match.getTeam1().getId(), 1);
                if (match.getTeam2() != null) teamSlotByTeamId.put(match.getTeam2().getId(), 2);
                if (match.getTeam3() != null) teamSlotByTeamId.put(match.getTeam3().getId(), 3);
                if (match.getTeam4() != null) teamSlotByTeamId.put(match.getTeam4().getId(), 4);
            }

            Integer[] teamScores = new Integer[4];
            Boolean[] teamWons = new Boolean[4];

            if (result.teamResults() != null) {
                for (var teamResult : result.teamResults()) {
                    Integer slot = teamSlotByTeamId.get(teamResult.teamId());
                    if (slot == null) continue;
                    int idx = slot - 1;
                    List<ParticipantScoreDto> ps = teamResult.participantScores();
                    teamScores[idx] = ps.stream()
                            .map(ParticipantScoreDto::score)
                            .filter(Objects::nonNull)
                            .reduce(0, Integer::sum);
                    teamWons[idx] = teamResult.won();
                }
            }

            for (int i = 0; i < 4; i++) {
                objectNode.put("team" + (i + 1) + "score", teamScores[i]);
                objectNode.put("team" + (i + 1) + "won", teamWons[i]);
            }

            Map<Long, Integer> debaterScoreById = new HashMap<>();
            if (result.participantScores() != null) {
                result.participantScores().forEach(ps -> {
                    if (ps.participantId() != null && ps.score() != null) {
                        debaterScoreById.put(ps.participantId(), ps.score());
                    }
                });
            }
            Long debater1Id = match != null && match.getDebater1() != null ? match.getDebater1().getId() : null;
            Long debater2Id = match != null && match.getDebater2() != null ? match.getDebater2().getId() : null;
            objectNode.put("debater1score", debater1Id != null ? debaterScoreById.get(debater1Id) : null);
            objectNode.put("debater2score", debater2Id != null ? debaterScoreById.get(debater2Id) : null);

            arrayNode.add(objectNode);
        });

        String json = arrayNode.toString();
        matchRepository.updateMatchScoresBulk(json);

        results.forEach(result -> {
            if (result.teamResults() == null) return;
            result.teamResults().forEach(teamResult -> {
                if (teamResult.participantScores() == null) return;
                teamResult.participantScores().forEach(ps -> {
                    if (ps.participantId() != null && ps.score() != null) {
                        tournamentParticipantRepository.addSpeakerScore(ps.participantId(), ps.score());
                    }
                });
            });
        });
    }
}
