package com.heliozz10.debetter.service.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.service.tournament.MatchParticipantScorePolicy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class RoundGroupService {

    private final RoundGroupRepository roundGroupRepository;

    private final RoundService roundService;
    private final RoundRepository roundRepository;

    private final TeamRepository teamRepository;
    @Transactional(readOnly = true)
    public List<RoundGroup> getRoundGroupsByTournamentId(Long tournamentId) {
        return roundGroupRepository.findByTournamentId(tournamentId);
    }

    @Transactional(readOnly = true)
    public RoundGroup getRoundGroupByTournamentIdAndId(Long tournamentId, Long id) {
        return roundGroupRepository.findByTournamentIdAndId(tournamentId, id)
                .orElseThrow(() -> new EntityNotFoundException("Round group not found"));
    }

    @Transactional
    public void changeRoundGroupFormat(RoundGroupType roundGroupType, DebateFormat format, Long tournamentId) {
        roundGroupRepository.changeRoundGroupFormat(roundGroupType, format, tournamentId);
    }

    /**
     * Proceeds to the next round of the round group. If the current round is not completed, throws an exception.
     * @param roundGroupId
     */
    @Transactional
    public void proceedToNextRound(Long tournamentId, Long roundGroupId) {
        RoundGroup roundGroup = roundGroupRepository.findFullByTournamentIdAndId(tournamentId, roundGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Round group not found"));

        Tournament tournament = roundGroup.getTournament();

        if(!tournament.getStarted()) {
            throw new IllegalStateException("Tournament is not started");
        }

        Round currentRound = roundRepository.findWithTeamsByRoundGroup_IdAndRoundNumber(roundGroupId, roundGroup.getCurrentRoundNumber())
                .orElseThrow(() -> new EntityNotFoundException("Current round not found"));

        if(!roundRepository.areAllMatchesCompleted(currentRound)) {
            throw new IllegalStateException("Current round is not completed");
        }

        int roundCount = roundGroup.getRounds().size();
        int currentRoundNumber = roundGroup.getCurrentRoundNumber();
        int nextRoundNumber = currentRoundNumber + 1;

        if(roundGroup.getType() == RoundGroupType.PRELIMINARY) {
            if(currentRoundNumber >= roundCount) {
                startEliminationRounds(tournament);
                return;
            }

            Round nextRound = roundRepository.findByRoundGroup_IdAndRoundNumber(roundGroupId, nextRoundNumber)
                    .orElseThrow(() -> new EntityNotFoundException("Next round not found"));

            if(roundFormat(nextRound) == DebateFormat.LD) {
                roundService.setDebaters(nextRound, eligibleDebaters(tournament));
            } else {
                roundService.setTeams(nextRound, teamRepository.findByTournamentAndDisqualifiedFalse(tournament));
            }

            roundService.generateMatchesAndAssignJudges(nextRound);

            roundGroup.setCurrentRoundNumber(nextRoundNumber);
            roundGroupRepository.save(roundGroup);

            return;
        }

        if(currentRoundNumber >= roundCount) {
            throw new IllegalStateException("Round group is completed");
        }

        Round nextRound = roundRepository.findByRoundGroup_IdAndRoundNumber(roundGroupId, nextRoundNumber)
                .orElseThrow(() -> new EntityNotFoundException("Next round not found"));

        if(roundGroup.getType() == RoundGroupType.TEAM_ELIMINATION) {
            List<Team> winners = roundService.getMatchWinnerTeams(currentRound.getId());
            assertExpectedQualifierCount(
                    winners,
                    entrantsForRound(roundCount - currentRoundNumber, roundFormat(nextRound))
            );
            roundService.setTeams(nextRound, winners);
        }

        if(roundGroup.getType() == RoundGroupType.SOLO_ELIMINATION) {
            List<TournamentParticipant> winners = roundService.getMatchWinnerDebaters(currentRound.getId());
            assertExpectedQualifierCount(winners, entrantsForRound(roundCount - currentRoundNumber, DebateFormat.LD));
            roundService.setDebaters(nextRound, winners);
        }

        roundService.generateMatchesAndAssignJudges(nextRound);

        roundGroup.setCurrentRoundNumber(nextRoundNumber);
        roundGroupRepository.save(roundGroup);
    }

    /**
     * Sets the teams of the first rounds of the elimination rounds.
     * LD - selects the top debaters based on their speaker score
     * Other formats - selects the top teams based on their score
     * @param tournament
     */
    private void startEliminationRounds(Tournament tournament) {
        RoundGroup teamEliminationRoundGroup = tournament.getRoundGroups().stream().filter(roundGroup -> roundGroup.getType() == RoundGroupType.TEAM_ELIMINATION).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Team elimination round group not found"));
        Optional<RoundGroup> soloEliminationRoundGroup = tournament.getRoundGroups().stream()
                .filter(roundGroup -> roundGroup.getType() == RoundGroupType.SOLO_ELIMINATION)
                .findFirst();

        validatePreliminaryTeamResults(tournament);
        soloEliminationRoundGroup.ifPresent(ignored -> validatePreliminarySpeakerScores(tournament));

        Round teamEliminationFirstRound = roundRepository.findByRoundGroup_IdAndRoundNumber(teamEliminationRoundGroup.getId(), 1)
                .orElseThrow(() -> new EntityNotFoundException("Team elimination first round not found"));

        if(teamEliminationRoundGroup.getCurrentRoundNumber() != null) {
            throw new IllegalStateException("Team elimination rounds have already started.");
        }

        int teamEntrants = entrantsForRound(
                teamEliminationRoundGroup.getRounds().size(),
                teamEliminationRoundGroup.getFormat()
        );

        List<Team> topTeams = teamRepository.findByTournamentAndDisqualifiedFalse(tournament).stream()
                .sorted(Comparator.comparing(Team::getPreliminaryScore, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(teamEntrants)
                .toList();

        if(teamEntrants != topTeams.size()) {
            throw new IllegalStateException("Not enough teams");
        }

        roundService.setTeams(teamEliminationFirstRound, topTeams);
        roundService.generateMatchesAndAssignJudges(teamEliminationFirstRound);

        teamEliminationRoundGroup.setCurrentRoundNumber(1);
        roundGroupRepository.save(teamEliminationRoundGroup);

        if(soloEliminationRoundGroup.isEmpty()) {
            return;
        }

        RoundGroup soloGroup = soloEliminationRoundGroup.get();

        Round soloEliminationFirstRound = roundRepository.findByRoundGroup_IdAndRoundNumber(soloGroup.getId(), 1)
                .orElseThrow(() -> new EntityNotFoundException("Solo elimination first round not found"));

        if(soloGroup.getCurrentRoundNumber() != null) {
            throw new IllegalStateException("Solo elimination rounds have already started.");
        }

        int soloEntrants = entrantsForRound(soloGroup.getRounds().size(), DebateFormat.LD);
        List<TournamentParticipant> topDebaters = eligibleDebaters(tournament).stream()
                .sorted(Comparator.comparing(TournamentParticipant::getSpeakerScore, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(soloEntrants)
                .toList();

        if(soloEntrants != topDebaters.size()) {
            throw new IllegalStateException("Not enough debaters");
        }

        roundService.setDebaters(soloEliminationFirstRound, topDebaters);
        roundService.generateMatchesAndAssignJudges(soloEliminationFirstRound);

        soloGroup.setCurrentRoundNumber(1);
        roundGroupRepository.save(soloGroup);
    }

    private void validatePreliminarySpeakerScores(Tournament tournament) {
        RoundGroup preliminaryGroup = tournament.getRoundGroups().stream()
                .filter(roundGroup -> roundGroup.getType() == RoundGroupType.PRELIMINARY)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Preliminary round group not found"));

        List<Match> preliminaryMatches = preliminaryMatches(preliminaryGroup);
        boolean missingScores = preliminaryMatches.isEmpty()
                || preliminaryMatches.stream().anyMatch(this::hasMissingParticipantScores)
                || eligibleDebaters(tournament).stream().anyMatch(participant -> participant.getSpeakerScore() == null);

        if(missingScores) {
            throw new IllegalStateException("Cannot generate LD bracket while preliminary speaker points are missing.");
        }
    }

    private void validatePreliminaryTeamResults(Tournament tournament) {
        RoundGroup preliminaryGroup = tournament.getRoundGroups().stream()
                .filter(roundGroup -> roundGroup.getType() == RoundGroupType.PRELIMINARY)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Preliminary round group not found"));

        boolean invalid = preliminaryMatches(preliminaryGroup).stream()
                .filter(MatchParticipantScorePolicy::isTeamFormat)
                .anyMatch(match -> !MatchParticipantScorePolicy.hasExpectedTeamSlots(match)
                        || !MatchParticipantScorePolicy.hasValidWinnerFlags(match));
        if(invalid) {
            throw new IllegalStateException("Cannot advance while preliminary team winners are invalid.");
        }
    }

    private List<Match> preliminaryMatches(RoundGroup preliminaryGroup) {
        return Optional.ofNullable(preliminaryGroup.getRounds()).orElse(List.of()).stream()
                .flatMap(round -> Optional.ofNullable(round.getMatches()).orElse(List.of()).stream())
                .filter(match -> !Boolean.TRUE.equals(match.getIsBye()))
                .toList();
    }

    private boolean hasMissingParticipantScores(Match match) {
        return !Boolean.TRUE.equals(match.getCompleted())
                || !MatchParticipantScorePolicy.hasCompleteParticipantScores(match);
    }

    private DebateFormat roundFormat(Round round) {
        return round.getCustomFormat() != null ? round.getCustomFormat() : round.getRoundGroup().getFormat();
    }

    private List<TournamentParticipant> eligibleDebaters(Tournament tournament) {
        return Optional.ofNullable(tournament.getTeams()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(team -> !Boolean.TRUE.equals(team.getDisqualified()))
                .map(Team::getMembers)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    private int entrantsForRound(int remainingRounds, DebateFormat format) {
        int exponent = remainingRounds + (format == DebateFormat.BPF ? 1 : 0);
        if(remainingRounds < 1 || exponent > 30) {
            throw new IllegalStateException("Elimination round count must be between 1 and 30.");
        }
        return 1 << exponent;
    }

    private <T> void assertExpectedQualifierCount(List<T> qualifiers, int expectedCount) {
        if(qualifiers.size() != expectedCount || qualifiers.stream().distinct().count() != expectedCount) {
            throw new IllegalStateException("Completed matches did not produce the expected unique qualifiers.");
        }
    }
}
