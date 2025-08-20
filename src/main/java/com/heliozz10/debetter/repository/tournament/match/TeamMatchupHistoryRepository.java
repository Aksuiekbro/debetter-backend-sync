package com.heliozz10.debetter.repository.tournament.match;

import com.heliozz10.debetter.content.tournament.match.TeamMatchupHistory;
import com.heliozz10.debetter.content.tournament.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMatchupHistoryRepository extends JpaRepository<TeamMatchupHistory, Long> {
    List<TeamMatchupHistory> findByTeam1InAndTeam2In(Collection<Team> teams1, Collection<Team> teams2);
    Optional<TeamMatchupHistory> findByTeam1AndTeam2(Team team1, Team team2);
}
