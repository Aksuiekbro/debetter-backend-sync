package com.heliozz10.debetter.service.tournament;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.tournament.match.in.MatchLocationDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchResultDto;
import com.heliozz10.debetter.dto.tournament.match.in.MatchUpdateDto;
import com.heliozz10.debetter.dto.tournament.match.in.ParticipantScoreDto;
import com.heliozz10.debetter.dto.tournament.match.in.TeamResultDto;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchParticipantScoreRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final MatchParticipantScoreRepository matchParticipantScoreRepository;
    private final TournamentSecurity tournamentSecurity;

    private final ObjectMapper objectMapper;

    // Unsorted pages come back in unspecified SQL order, so rows can shuffle
    // between refetches; default to a stable id order.
    private Pageable withStableOrder(Pageable pageable) {
        if (pageable.getSort().isSorted()) return pageable;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public Page<Match> getMatchesByRoundId(Long roundId, Pageable pageable) {
        return matchRepository.findByRoundId(roundId, withStableOrder(pageable));
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
            return matchRepository.findByRoundId(roundId, withStableOrder(pageable));
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

    @Transactional
    public void updateMatchLocations(
            Long tournamentId,
            Long roundGroupId,
            Long roundId,
            Collection<MatchLocationDto> locations
    ) {
        if(locations == null || locations.isEmpty()) {
            return;
        }

        List<Long> allMatchIds = locations.stream()
                .map(MatchLocationDto::matchId)
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
            throw new IllegalArgumentException("Some matches do not belong to the specified round.");
        }

        Map<Long, Match> matchesById = matchRepository.findAllByIdForUpdate(matchIds).stream()
                .collect(Collectors.toMap(Match::getId, m -> m));

        if(matchesById.size() != matchIds.size()) {
            throw new IllegalArgumentException("Some matches do not belong to the specified round.");
        }

        if(matchesById.values().stream().anyMatch(match -> Boolean.TRUE.equals(match.getCompleted()))) {
            throw new IllegalStateException("Cannot edit a completed match");
        }

        locations.forEach(location -> matchesById.get(location.matchId())
                .setLocation(normalizeLocation(location.location())));

        matchRepository.saveAll(matchesById.values());
    }

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

        Map<Long, Match> matchesById = matchRepository.findAllByIdForUpdate(matchIds).stream()
                .collect(Collectors.toMap(Match::getId, m -> m));

        Map<Long, Boolean> legacyRepairsByMatchId = new HashMap<>();
        results.forEach(result -> {
            Match match = matchesById.get(result.matchId());
            validateCompleteBallot(match, result);

            boolean legacyRepair = isLegacyTeamScoreRepair(match);
            if(Boolean.TRUE.equals(match.getCompleted()) && !legacyRepair) {
                throw new IllegalStateException("Cannot re-submit results for already completed matches.");
            }
            if(legacyRepair) {
                validateLegacyTeamScoreRepair(match, result);
            }
            legacyRepairsByMatchId.put(result.matchId(), legacyRepair);
        });

        List<MatchResultDto> newResults = results.stream()
                .filter(result -> !legacyRepairsByMatchId.get(result.matchId()))
                .toList();

        if(!newResults.isEmpty()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();

            newResults.forEach(result -> {
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
                        teamScores[idx] = ps == null || ps.isEmpty()
                                ? null
                                : ps.stream()
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

            matchRepository.updateMatchScoresBulk(arrayNode.toString());

            newResults.forEach(result -> {
                if(result.winnerParticipantId() != null) {
                    matchRepository.updateWinnerParticipantId(result.matchId(), result.winnerParticipantId());
                }
            });
        }

        results.forEach(result -> {
            if (result.teamResults() == null) return;
            Match match = matchesById.get(result.matchId());
            if(!isPreliminaryMatch(match) && !Boolean.TRUE.equals(legacyRepairsByMatchId.get(result.matchId()))) {
                return;
            }
            persistTeamParticipantScores(match, result.teamResults());
            if(Boolean.TRUE.equals(legacyRepairsByMatchId.get(result.matchId()))) {
                return;
            }
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

    private boolean isPreliminaryMatch(Match match) {
        return MatchParticipantScorePolicy.isPreliminaryMatch(match);
    }

    private boolean isLegacyTeamScoreRepair(Match match) {
        if(match == null
                || !isPreliminaryMatch(match)
                || !Boolean.TRUE.equals(match.getCompleted())
                || !MatchParticipantScorePolicy.isTeamFormat(match)
                || !MatchParticipantScorePolicy.hasRepairableAggregateResult(match)) {
            return false;
        }

        return matchParticipantScoreRepository.countByMatchId(match.getId()) == 0;
    }

    private void validateLegacyTeamScoreRepair(Match match, MatchResultDto result) {
        if (!MatchParticipantScorePolicy.hasRepairableAggregateResult(match)) {
            throw new IllegalArgumentException("Legacy participant-score repair requires completed team totals and winners.");
        }

        for(TeamResultDto teamResult : result.teamResults()) {
            int slot = teamSlot(match, teamResult.teamId());
            int suppliedScore = teamResult.participantScores().stream()
                    .mapToInt(ParticipantScoreDto::score)
                    .sum();
            if(!Objects.equals(teamScore(match, slot), suppliedScore)
                    || !Objects.equals(teamWon(match, slot), teamResult.won())) {
                throw new IllegalArgumentException("Legacy participant-score repair must preserve the completed team total and winner.");
            }
        }
    }

    private void persistTeamParticipantScores(Match match, List<TeamResultDto> teamResults) {
        Map<Long, TournamentParticipant> participantsById = Stream.of(
                        match.getTeam1(), match.getTeam2(), match.getTeam3(), match.getTeam4())
                .filter(Objects::nonNull)
                .map(Team::getMembers)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(TournamentParticipant::getId, participant -> participant));
        List<MatchParticipantScore> scores = teamResults.stream()
                .flatMap(teamResult -> teamResult.participantScores().stream())
                .map(participantScore -> {
                    MatchParticipantScore score = new MatchParticipantScore();
                    score.setMatch(match);
                    score.setParticipant(participantsById.get(participantScore.participantId()));
                    score.setScore(participantScore.score());
                    return score;
                })
                .toList();

        if(match.getParticipantScores() == null) {
            match.setParticipantScores(new ArrayList<>());
        }
        match.getParticipantScores().addAll(scores);
        matchParticipantScoreRepository.saveAll(scores);
    }

    private DebateFormat resolveFormat(Match match) {
        return MatchParticipantScorePolicy.resolveFormat(match);
    }

    private int teamSlot(Match match, Long teamId) {
        if(Objects.equals(match.getTeam1() == null ? null : match.getTeam1().getId(), teamId)) return 1;
        if(Objects.equals(match.getTeam2() == null ? null : match.getTeam2().getId(), teamId)) return 2;
        if(Objects.equals(match.getTeam3() == null ? null : match.getTeam3().getId(), teamId)) return 3;
        if(Objects.equals(match.getTeam4() == null ? null : match.getTeam4().getId(), teamId)) return 4;
        throw new IllegalArgumentException("Team result does not belong to the match.");
    }

    private Integer teamScore(Match match, int slot) {
        return switch(slot) {
            case 1 -> match.getTeam1Score();
            case 2 -> match.getTeam2Score();
            case 3 -> match.getTeam3Score();
            case 4 -> match.getTeam4Score();
            default -> throw new IllegalArgumentException("Invalid team slot.");
        };
    }

    private Boolean teamWon(Match match, int slot) {
        return switch(slot) {
            case 1 -> match.getTeam1Won();
            case 2 -> match.getTeam2Won();
            case 3 -> match.getTeam3Won();
            case 4 -> match.getTeam4Won();
            default -> throw new IllegalArgumentException("Invalid team slot.");
        };
    }

    private void validateCompleteBallot(Match match, MatchResultDto result) {
        if(match == null) {
            throw new IllegalArgumentException("Match result could not be resolved.");
        }

        DebateFormat format = resolveFormat(match);

        if(format == DebateFormat.LD) {
            if(result.teamResults() != null && !result.teamResults().isEmpty()) {
                throw new IllegalArgumentException("LD results cannot contain team results.");
            }
            if(match.getDebater1() == null || match.getDebater2() == null) {
                throw new IllegalStateException("LD match does not have two debater slots.");
            }

            if(isPreliminaryMatch(match)) {
                if(result.winnerParticipantId() != null) {
                    throw new IllegalArgumentException("Preliminary LD results cannot identify a winner without speaker points.");
                }

                List<ParticipantScoreDto> scores = result.participantScores();
                validateParticipantScores(scores, List.of(match.getDebater1(), match.getDebater2()));
                Map<Long, Integer> scoreByParticipant = scores.stream()
                        .collect(Collectors.toMap(ParticipantScoreDto::participantId, ParticipantScoreDto::score));
                if(Objects.equals(
                        scoreByParticipant.get(match.getDebater1().getId()),
                        scoreByParticipant.get(match.getDebater2().getId()))) {
                    throw new IllegalArgumentException("LD results cannot be tied.");
                }
                return;
            }

            RoundGroupType roundGroupType = match.getRound().getRoundGroup().getType();
            if(roundGroupType != RoundGroupType.SOLO_ELIMINATION) {
                throw new IllegalStateException("LD Win/Loss results are only supported in solo elimination.");
            }

            if(result.participantScores() != null && !result.participantScores().isEmpty()) {
                throw new IllegalArgumentException("LD elimination results must not contain speaker points.");
            }
            if(result.winnerParticipantId() == null
                    || (!Objects.equals(result.winnerParticipantId(), match.getDebater1().getId())
                        && !Objects.equals(result.winnerParticipantId(), match.getDebater2().getId()))) {
                throw new IllegalArgumentException("LD results must identify exactly one participating winner.");
            }
            return;
        }

        if(result.winnerParticipantId() != null) {
            throw new IllegalArgumentException("Team-format results cannot identify an individual winner.");
        }

        if(result.participantScores() != null && !result.participantScores().isEmpty()) {
            throw new IllegalArgumentException("Team-format results must nest participant scores under each team.");
        }

        if(format == null) {
            throw new IllegalStateException("Match format is not configured.");
        }

        int requiredTeams = format == DebateFormat.BPF ? 4 : 2;
        List<Team> teams = MatchParticipantScorePolicy.expectedTeams(match);
        if(teams.size() != requiredTeams) {
            throw new IllegalStateException("Match does not have the required team slots for its format.");
        }

        List<TeamResultDto> teamResults = result.teamResults();
        if(teamResults == null || teamResults.size() != requiredTeams) {
            throw new IllegalArgumentException("A result is required for every team in the match.");
        }

        Set<Long> expectedTeamIds = teams.stream().map(Team::getId).collect(Collectors.toSet());
        Set<Long> suppliedTeamIds = teamResults.stream().map(TeamResultDto::teamId).collect(Collectors.toSet());
        if(suppliedTeamIds.size() != teamResults.size() || !suppliedTeamIds.equals(expectedTeamIds)) {
            throw new IllegalArgumentException("Team results must identify each match team exactly once.");
        }

        if(teamResults.stream().anyMatch(teamResult -> teamResult.won() == null)) {
            throw new IllegalArgumentException("Every team must have an explicit winner result.");
        }

        long winnerCount = teamResults.stream().filter(teamResult -> Boolean.TRUE.equals(teamResult.won())).count();
        int requiredWinnerCount = format == DebateFormat.BPF ? 2 : 1;
        if(winnerCount != requiredWinnerCount) {
            throw new IllegalArgumentException("Exactly " + requiredWinnerCount + " teams must be marked as winners.");
        }

        if(isPreliminaryMatch(match)) {
            Map<Long, Team> teamsById = teams.stream().collect(Collectors.toMap(Team::getId, team -> team));
            teamResults.forEach(teamResult -> validateParticipantScores(
                    teamResult.participantScores(),
                    Optional.ofNullable(teamsById.get(teamResult.teamId()).getMembers()).orElse(List.of())
            ));
            return;
        }

        if(teamResults.stream().anyMatch(teamResult ->
                teamResult.participantScores() != null && !teamResult.participantScores().isEmpty())) {
            throw new IllegalArgumentException("Team elimination results must not contain speaker points.");
        }
    }

    private void validateParticipantScores(List<ParticipantScoreDto> scores, List<TournamentParticipant> participants) {
        if(scores == null || participants == null || participants.isEmpty() || scores.size() != participants.size()) {
            throw new IllegalArgumentException("A score is required for every participating debater.");
        }

        Set<Long> expectedParticipantIds = participants.stream()
                .map(TournamentParticipant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> suppliedParticipantIds = scores.stream()
                .map(ParticipantScoreDto::participantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasInvalidScore = scores.stream().anyMatch(score -> score.score() == null || score.score() < 0);

        if(expectedParticipantIds.size() != participants.size()
                || suppliedParticipantIds.size() != scores.size()
                || !suppliedParticipantIds.equals(expectedParticipantIds)
                || hasInvalidScore) {
            throw new IllegalArgumentException("Participant scores must cover each match debater exactly once.");
        }
    }
}
