package com.heliozz10.debetter.service.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.DebaterMatchupHistory;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.TeamMatchupHistory;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.dto.tournament.round.in.RoundUpdateDto;
import com.heliozz10.debetter.mapper.tournament.round.RoundMapper;
import com.heliozz10.debetter.repository.tournament.JudgeRepository;
import com.heliozz10.debetter.repository.tournament.match.DebaterMatchupHistoryRepository;
import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.repository.tournament.match.TeamMatchupHistoryRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class RoundService {
    private final RoundRepository roundRepository;
    private final RoundMapper roundMapper;

    private final MatchRepository matchRepository;
    private final JudgeRepository judgeRepository;

    private final TeamMatchupHistoryRepository teamMatchupHistoryRepository;
    private final DebaterMatchupHistoryRepository debaterMatchupHistoryRepository;

    @Transactional(readOnly = true)
    public List<Team> getMatchWinnerTeams(Long roundId) {
        return matchRepository.findByRoundId(roundId).stream()
                .flatMap(match -> Stream.of(
                        Boolean.TRUE.equals(match.getTeam1Won()) ? match.getTeam1() : null,
                        Boolean.TRUE.equals(match.getTeam2Won()) ? match.getTeam2() : null,
                        Boolean.TRUE.equals(match.getTeam3Won()) ? match.getTeam3() : null,
                        Boolean.TRUE.equals(match.getTeam4Won()) ? match.getTeam4() : null
                ))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TournamentParticipant> getMatchWinnerDebaters(Long roundId) {
        return matchRepository.findByRoundId(roundId).stream()
                .map(match -> {
                    if (match.getDebater1Score() != null && match.getDebater2Score() != null) {
                        return match.getDebater1Score() >= match.getDebater2Score()
                                ? match.getDebater1() : match.getDebater2();
                    }
                    return match.getDebater1Score() != null ? match.getDebater1() : match.getDebater2();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Round> getRoundsByTournamentIdAndRoundGroupId(Long tournamentId, Long roundGroupId) {
        return roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_Id(tournamentId, roundGroupId);
    }

    @Transactional(readOnly = true)
    public Round getRoundByTournamentIdAndRoundGroupIdAndId(Long tournamentId, Long roundGroupId, Long id) {
        return roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(tournamentId, roundGroupId, id)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));
    }

    @Transactional
    public void updateRound(RoundUpdateDto roundUpdateDto, Long tournamentId, Long roundId) {
        Round round = roundRepository.findByRoundGroup_Tournament_IdAndId(tournamentId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        roundMapper.updateRound(roundUpdateDto, round);

        if(roundUpdateDto.customFormat() != null) {
            if(!round.getMatches().isEmpty()) {
                throw new IllegalStateException("Cannot update custom format for a round with matches");
            }

            round.setCustomFormat(roundUpdateDto.customFormat());
        }
    }

    @Transactional
    public void generateMatchesAndAssignJudges(Round round) {
        generateMatches(round);
        assignJudges(round);
    }

    @Transactional
    public void regenerateMatches(Long tournamentId, Long roundGroupId, Long roundId) {
        Round round = roundRepository.findWithPairingStateByTournamentAndRoundGroupAndId(tournamentId, roundGroupId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        clearMatches(round);
        generateMatchesAndAssignJudges(round);
    }

    @Transactional
    public void publishMatches(Long tournamentId, Long roundGroupId, Long roundId) {
        Round round = roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(tournamentId, roundGroupId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        if(round.getMatches() == null || round.getMatches().isEmpty()) {
            throw new IllegalStateException("Cannot publish pairings before matches are generated");
        }

        round.setMatchesArePublic(true);
    }

    @Transactional
    public void clearMatches(Long tournamentId, Long roundGroupId, Long roundId) {
        Round round = roundRepository.findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(tournamentId, roundGroupId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        clearMatches(round);
    }

    private void clearMatches(Round round) {
        if(round.getMatches() == null) {
            round.setMatches(new ArrayList<>());
        }

        if(round.getMatches().stream().anyMatch(match -> Boolean.TRUE.equals(match.getCompleted()))) {
            throw new IllegalStateException("Cannot clear matches after results are submitted");
        }

        round.getMatches().clear();
        round.setMatchesArePublic(false);
        roundRepository.flush();
    }

    //TODO: automatically assign judges. Done
    /**
     * Generates matches for a round. This method works with teams already set for the round. Does not check if teams are eligible.
     * So this method should only be called when teams are already set for the round and are eligible.
     * <p>
     * Used internally as a method that just generates matches
     * @param round The round to generate matches for
     */
    @Transactional
    public void generateMatches(Round round) {
        DebateFormat format = Optional.ofNullable(round.getCustomFormat())
                .orElse(round.getRoundGroup().getFormat());

        if (format == DebateFormat.LD) {
            generateMatchesGeneric(
                    round,
                    new ArrayList<>(round.getDebaters()),
                    2,
                    this::loadDebaterHistoryData,
                    this::createDebaterHistoryEntity,
                    this::setDebatersOnMatch,
                    debaterMatchupHistoryRepository::saveAll
            );
        } else {
            int groupSize = (format == DebateFormat.BPF) ? 4 : 2;
            generateMatchesGeneric(
                    round,
                    new ArrayList<>(round.getTeams()),
                    groupSize,
                    this::loadTeamHistoryData,
                    this::createTeamHistoryEntity,
                    this::setTeamsOnMatch,
                    teamMatchupHistoryRepository::saveAll
            );
        }
    }

    /** -------- Generic generator -------- */
    private <E, H> void generateMatchesGeneric(
            Round round,
            List<E> entities,
            int groupSize,
            java.util.function.Function<List<E>, Map<Long, H>> historyLoader,
            BiFunction<E, E, H> newHistoryCreator,
            BiConsumer<Match, List<E>> matchSetter,
            Consumer<List<H>> batchSaver
    ) {
        Collections.shuffle(entities);

        // Load all histories into memory
        Map<Long, H> histories = historyLoader.apply(entities);
        Map<Long, Integer> scores = extractTimesFaced(histories);

        Set<Long> used = new HashSet<>();
        List<List<E>> groups = new ArrayList<>();

        // Greedy grouping
        for (E first : entities) {
            long firstId = getId(first);
            if (used.contains(firstId)) continue;

            List<E> group = new ArrayList<>();
            group.add(first);
            used.add(firstId);

            while (group.size() < groupSize) {
                E best = null;
                int bestScore = Integer.MAX_VALUE;

                for (E candidate : entities) {
                    long candId = getId(candidate);
                    if (used.contains(candId)) continue;

                    int score = 0;
                    for (E member : group) {
                        score += getScore(getId(member), candId, scores);
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        best = candidate;
                    }
                }

                if (best != null) {
                    group.add(best);
                    used.add(getId(best));
                } else break;
            }

            if (group.size() == groupSize) {
                groups.add(group);
            }
        }

        // Create matches + update histories
        List<Match> matches = new ArrayList<>();
        for (List<E> group : groups) {
            Match match = new Match();
            match.setRound(round);
            match.setCompleted(false);
            matchSetter.accept(match, group);
            matches.add(match);

            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    long k = key(getId(group.get(i)), getId(group.get(j)));
                    scores.put(k, scores.getOrDefault(k, 0) + 1);

                    // If history doesn’t exist, create it
                    int finalI = i;
                    int finalJ = j;
                    histories.computeIfAbsent(k, unused -> newHistoryCreator.apply(group.get(finalI), group.get(finalJ)));
                    // Update entity’s counter
                    setTimesFaced(histories.get(k), scores.get(k));
                }
            }
        }

        matchRepository.saveAll(matches);
        batchSaver.accept(new ArrayList<>(histories.values()));
    }

    /** -------- History loading -------- */
    private Map<Long, TeamMatchupHistory> loadTeamHistoryData(List<?> teams) {
        @SuppressWarnings("unchecked")
        List<Team> tlist = (List<Team>) teams;
        Map<Long, TeamMatchupHistory> map = new HashMap<>();
        for (TeamMatchupHistory h : teamMatchupHistoryRepository.findByTeam1InAndTeam2In(tlist, tlist)) {
            map.put(key(h.getTeam1().getId(), h.getTeam2().getId()), h);
        }
        return map;
    }

    private Map<Long, DebaterMatchupHistory> loadDebaterHistoryData(List<?> debaters) {
        @SuppressWarnings("unchecked")
        List<TournamentParticipant> dlist = (List<TournamentParticipant>) debaters;
        Map<Long, DebaterMatchupHistory> map = new HashMap<>();
        for (DebaterMatchupHistory h : debaterMatchupHistoryRepository.findByDebater1InAndDebater2In(dlist, dlist)) {
            map.put(key(h.getDebater1().getId(), h.getDebater2().getId()), h);
        }
        return map;
    }

    /** -------- New history creators -------- */
    private TeamMatchupHistory createTeamHistoryEntity(Object a, Object b) {
        Team t1 = (Team) a;
        Team t2 = (Team) b;
        return (t1.getId() < t2.getId())
                ? new TeamMatchupHistory(t1, t2, 0)
                : new TeamMatchupHistory(t2, t1, 0);
    }

    private DebaterMatchupHistory createDebaterHistoryEntity(Object a, Object b) {
        TournamentParticipant p1 = (TournamentParticipant) a;
        TournamentParticipant p2 = (TournamentParticipant) b;
        return (p1.getId() < p2.getId())
                ? new DebaterMatchupHistory(p1, p2, 0)
                : new DebaterMatchupHistory(p2, p1, 0);
    }

    private void setTeamsOnMatch(Match match, List<?> group) {
        @SuppressWarnings("unchecked")
        List<Team> g = (List<Team>) group;
        match.setTeam1(g.get(0));
        match.setTeam2(g.get(1));
        if (g.size() == 4) {
            match.setTeam3(g.get(2));
            match.setTeam4(g.get(3));
        }
    }

    private void setDebatersOnMatch(Match match, List<?> group) {
        @SuppressWarnings("unchecked")
        List<TournamentParticipant> g = (List<TournamentParticipant>) group;
        match.setDebater1(g.get(0));
        match.setDebater2(g.get(1));
    }

    /** helper stuff -------- */
    private static long key(long a, long b) {
        return (Math.min(a, b) << 32) | Math.max(a, b);
    }

    private static int getScore(long id1, long id2, Map<Long, Integer> scores) {
        return scores.getOrDefault(key(id1, id2), 0) * 100;
    }

    private static long getId(Object entity) {
        if (entity instanceof Team) return ((Team) entity).getId();
        if (entity instanceof TournamentParticipant) return ((TournamentParticipant) entity).getId();
        throw new IllegalArgumentException("Unsupported type: " + entity.getClass());
    }

    /** Extracts timesFaced integers from history map */
    private static <H> Map<Long, Integer> extractTimesFaced(Map<Long, H> histories) {
        Map<Long, Integer> scores = new HashMap<>();
        for (Map.Entry<Long, H> e : histories.entrySet()) {
            scores.put(e.getKey(), getTimesFaced(e.getValue()));
        }
        return scores;
    }

    /** Reflection-free counter getters/setters */
    private static int getTimesFaced(Object history) {
        if (history instanceof TeamMatchupHistory) return ((TeamMatchupHistory) history).getTimesFaced();
        if (history instanceof DebaterMatchupHistory) return ((DebaterMatchupHistory) history).getTimesFaced();
        throw new IllegalArgumentException("Unsupported history type");
    }

    private static void setTimesFaced(Object history, int times) {
        if (history instanceof TeamMatchupHistory) ((TeamMatchupHistory) history).setTimesFaced(times);
        else if (history instanceof DebaterMatchupHistory) ((DebaterMatchupHistory) history).setTimesFaced(times);
        else throw new IllegalArgumentException("Unsupported history type");
    }

    @Transactional
    public void assignJudges(Round round) {
        Long tournamentId = round.getRoundGroup().getTournament().getId();
        List<Judge> judges = judgeRepository.findByTournamentIdAndCheckedInTrueOrderByTimesJudgedAscIdAsc(tournamentId);
        if (judges.isEmpty()) {
            return;
        }

        List<Match> matches = matchRepository.findByRoundIdAndJudgeIsNullOrderByIdAsc(round.getId());
        if (matches.isEmpty()) {
            return;
        }

        Map<Judge, Integer> assignmentCounts = new HashMap<>();
        for (int i = 0; i < matches.size(); i++) {
            Judge judge = judges.get(i % judges.size());
            matches.get(i).setJudge(judge);
            assignmentCounts.merge(judge, 1, Integer::sum);
        }

        assignmentCounts.forEach((judge, count) -> {
            int previousCount = Optional.ofNullable(judge.getTimesJudged()).orElse(0);
            judge.setTimesJudged(previousCount + count);
        });

        matchRepository.saveAll(matches);
        judgeRepository.saveAll(judges);
    }

    public void setTeams(Round round, List<Team> teams) {
        round.setTeams(teams);
    }

    public void setDebaters(Round round, List<TournamentParticipant> debaters) {
        round.setDebaters(debaters);
    }

    public void deleteRound(Long tournamentId, Long roundId) {
        Round round = roundRepository.findByRoundGroup_Tournament_IdAndId(tournamentId, roundId)
                .orElseThrow(() -> new EntityNotFoundException("Round not found"));

        roundRepository.deleteById(roundId);
    }
}
