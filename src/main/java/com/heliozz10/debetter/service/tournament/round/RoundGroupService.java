package com.heliozz10.debetter.service.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundGroupRepository;
import com.heliozz10.debetter.repository.tournament.round.RoundRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RoundGroupService {

    private final RoundGroupRepository roundGroupRepository;

    private final RoundService roundService;
    private final RoundRepository roundRepository;

    private final TeamRepository teamRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;

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

            List<Team> teams = teamRepository.findByTournamentAndDisqualifiedFalse(tournament);

            roundService.setTeams(nextRound, teams);

            roundService.generateMatchesAndAssignJudges(nextRound);

            roundGroup.setCurrentRoundNumber(nextRoundNumber);
            roundGroupRepository.save(roundGroup);

            return;
        }

        if(currentRoundNumber >= roundCount) {
            throw new IllegalStateException("Round group is completed");
        }

        int numberOfEntrants = (int)Math.pow(2, roundCount - currentRoundNumber);

        Round nextRound = roundRepository.findByRoundGroup_IdAndRoundNumber(roundGroupId, nextRoundNumber)
                .orElseThrow(() -> new EntityNotFoundException("Next round not found"));

        if(roundGroup.getType() == RoundGroupType.TEAM_ELIMINATION) {
            List<Team> winners = roundService.getMatchWinnerTeams(currentRound.getId());
            roundService.setTeams(nextRound, winners);
        }

        if(roundGroup.getType() == RoundGroupType.SOLO_ELIMINATION) {
            List<TournamentParticipant> winners = roundService.getMatchWinnerDebaters(currentRound.getId());
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

        Round teamEliminationFirstRound = roundRepository.findByRoundGroup_IdAndRoundNumber(teamEliminationRoundGroup.getId(), 1)
                .orElseThrow(() -> new EntityNotFoundException("Team elimination first round not found"));

        int numberOfEntrants = (int)Math.pow(2, teamEliminationRoundGroup.getRounds().size());

        List<Team> topTeams = teamRepository.findByTournamentAndDisqualifiedFalse(tournament).stream()
                .sorted(Comparator.comparing(Team::getPreliminaryScore, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(numberOfEntrants)
                .toList();

        if(numberOfEntrants != topTeams.size()) {
            throw new IllegalStateException("Not enough teams");
        }

        roundService.setTeams(teamEliminationFirstRound, topTeams);

        teamEliminationRoundGroup.setCurrentRoundNumber(1);
        roundGroupRepository.save(teamEliminationRoundGroup);

        RoundGroup soloEliminationRoundGroup = tournament.getRoundGroups().stream().filter(roundGroup -> roundGroup.getType() == RoundGroupType.SOLO_ELIMINATION).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Solo elimination round group not found"));

        Round soloEliminationFirstRound = roundRepository.findByRoundGroup_IdAndRoundNumber(soloEliminationRoundGroup.getId(), 1)
                .orElseThrow(() -> new EntityNotFoundException("Solo elimination first round not found"));

        List<TournamentParticipant> topDebaters = tournament.getTeams().stream().flatMap(team -> team.getMembers().stream())
                .sorted(Comparator.comparing(TournamentParticipant::getSpeakerScore, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(numberOfEntrants)
                .toList();

        if(numberOfEntrants != topDebaters.size()) {
            throw new IllegalStateException("Not enough debaters");
        }

        roundService.setDebaters(soloEliminationFirstRound, topDebaters);

        soloEliminationRoundGroup.setCurrentRoundNumber(1);
        roundGroupRepository.save(soloEliminationRoundGroup);
    }
}
