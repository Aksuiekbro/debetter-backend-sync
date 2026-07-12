package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.team.Team;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared participant-score completeness and legacy-repair rules for team matches.
 */
public final class MatchParticipantScorePolicy {
    private MatchParticipantScorePolicy() {
    }

    public static DebateFormat resolveFormat(Match match) {
        if (match == null || match.getRound() == null || match.getRound().getRoundGroup() == null) {
            return null;
        }

        return Optional.ofNullable(match.getRound().getCustomFormat())
                .orElse(match.getRound().getRoundGroup().getFormat());
    }

    public static boolean isTeamFormat(Match match) {
        DebateFormat format = resolveFormat(match);
        return format != null && format != DebateFormat.LD;
    }

    public static int requiredTeamCount(Match match) {
        DebateFormat format = resolveFormat(match);
        if (format == DebateFormat.BPF) {
            return 4;
        }
        return isTeamFormat(match) ? 2 : 0;
    }

    public static int requiredWinnerCount(Match match) {
        return resolveFormat(match) == DebateFormat.BPF ? 2 : (isTeamFormat(match) ? 1 : 0);
    }

    public static List<Team> expectedTeams(Match match) {
        int requiredTeamCount = requiredTeamCount(match);
        if (requiredTeamCount == 0) {
            return List.of();
        }

        List<Team> slots = teamSlots(match);
        List<Team> requiredSlots = slots.subList(0, requiredTeamCount);
        if (requiredSlots.stream().anyMatch(Objects::isNull)
                || slots.subList(requiredTeamCount, slots.size()).stream().anyMatch(Objects::nonNull)) {
            return List.of();
        }

        Set<Long> teamIds = requiredSlots.stream()
                .map(Team::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return teamIds.size() == requiredTeamCount ? requiredSlots : List.of();
    }

    public static boolean hasExpectedTeamSlots(Match match) {
        return !expectedTeams(match).isEmpty();
    }

    public static boolean hasValidWinnerFlags(Match match) {
        if (!hasExpectedTeamSlots(match)) {
            return false;
        }

        int requiredTeamCount = requiredTeamCount(match);
        int requiredWinnerCount = requiredWinnerCount(match);
        List<Boolean> winners = winnerSlots(match);
        return winners.subList(0, requiredTeamCount).stream().allMatch(Objects::nonNull)
                && winners.subList(0, requiredTeamCount).stream().filter(Boolean.TRUE::equals).count() == requiredWinnerCount
                && winners.subList(requiredTeamCount, winners.size()).stream().allMatch(Objects::isNull);
    }

    public static List<TournamentParticipant> expectedParticipants(Match match) {
        return expectedTeams(match).stream()
                .map(Team::getMembers)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    public static boolean hasCompleteParticipantScores(Match match) {
        List<TournamentParticipant> expectedParticipants = expectedParticipants(match);
        List<MatchParticipantScore> scores = safeScores(match);
        Set<Long> expectedIds = expectedParticipants.stream()
                .map(TournamentParticipant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> scoredIds = scores.stream()
                .map(MatchParticipantScore::getParticipant)
                .filter(Objects::nonNull)
                .map(TournamentParticipant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return !expectedParticipants.isEmpty()
                && expectedIds.size() == expectedParticipants.size()
                && scores.size() == expectedParticipants.size()
                && scoredIds.size() == scores.size()
                && scoredIds.equals(expectedIds)
                && scores.stream().allMatch(score -> score.getScore() != null && score.getScore() >= 0);
    }

    public static boolean hasRepairableAggregateResult(Match match) {
        if (!Boolean.TRUE.equals(match == null ? null : match.getCompleted()) || !isTeamFormat(match)) {
            return false;
        }

        List<Team> teams = expectedTeams(match);
        List<TournamentParticipant> expectedParticipants = expectedParticipants(match);
        Set<Long> expectedParticipantIds = expectedParticipants.stream()
                .map(TournamentParticipant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int requiredTeamCount = requiredTeamCount(match);
        List<Integer> scores = Arrays.asList(
                match.getTeam1Score(),
                match.getTeam2Score(),
                match.getTeam3Score(),
                match.getTeam4Score()
        );
        List<Boolean> winners = Arrays.asList(
                match.getTeam1Won(),
                match.getTeam2Won(),
                match.getTeam3Won(),
                match.getTeam4Won()
        );

        return !teams.isEmpty()
                && !expectedParticipants.isEmpty()
                && expectedParticipantIds.size() == expectedParticipants.size()
                && hasExpectedTeamSlots(match)
                && scores.subList(0, requiredTeamCount).stream().allMatch(score -> score != null && score >= 0)
                && scores.subList(requiredTeamCount, scores.size()).stream().allMatch(Objects::isNull)
                && hasValidWinnerFlags(match);
    }

    public static boolean isRepairable(Match match) {
        return hasRepairableAggregateResult(match) && safeScores(match).isEmpty();
    }

    private static List<MatchParticipantScore> safeScores(Match match) {
        return match == null || match.getParticipantScores() == null ? List.of() : match.getParticipantScores();
    }

    private static List<Team> teamSlots(Match match) {
        return Stream.of(match.getTeam1(), match.getTeam2(), match.getTeam3(), match.getTeam4()).toList();
    }

    private static List<Boolean> winnerSlots(Match match) {
        return Stream.of(match.getTeam1Won(), match.getTeam2Won(), match.getTeam3Won(), match.getTeam4Won()).toList();
    }
}
