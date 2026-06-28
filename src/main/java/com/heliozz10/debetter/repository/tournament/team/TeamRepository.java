package com.heliozz10.debetter.repository.tournament.team;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @EntityGraph(value = "Team.full", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Team> findFullById(Long id);

    @EntityGraph(value = "Team.full", type = EntityGraph.EntityGraphType.LOAD)
    Page<Team> findByTournamentId(Long tournamentId, Pageable pageable);

    @Query("SELECT t FROM Team t WHERE t.tournament = :tournament AND (t.disqualified = false OR t.disqualified IS NULL)")
    List<Team> findByTournamentAndDisqualifiedFalse(@Param("tournament") Tournament tournament);

    List<Team> findByClubId(Long clubId);

    @Query("SELECT t.active FROM Team t WHERE t.id = :teamId")
    Boolean teamIsActiveById(Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.active = true WHERE t.id = :teamId")
    void setTeamActiveById(Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.active = false WHERE t.id = :teamId")
    void setTeamInactiveById(Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.checkedIn = true WHERE t.id = :teamId")
    void checkInTeamById(Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.checkedIn = false WHERE t.id = :teamId")
    void uncheckInTeamById(Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.disqualified = true WHERE t.id = :teamId AND t.tournament.id = :tournamentId")
    void setTeamDisqualifiedByTournamentIdAndId(Long tournamentId, Long teamId);

    @Modifying
    @Query("UPDATE Team t SET t.disqualified = false WHERE t.id = :teamId AND t.tournament.id = :tournamentId")
    void setTeamNotDisqualifiedByTournamentIdAndId(Long tournamentId, Long teamId);

    @Transactional
    @Modifying
    @Query("update Team t set t.name = ?1 where t.id = ?2")
    int updateNameById(String name, Long id);

    @Transactional
    @Modifying
    @Query("update Team t set t.name = ?1, t.club = ?2 where t.id = ?3")
    int updateNameAndClubById(String name, Club club, Long id);

    @EntityGraph(value = "Team.full", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Team> findByTournamentIdAndId(Long tournamentId, Long id);

    Team findByMembers_Id(Long id);

    Optional<Team> findByTournament_IdAndMembers_IdAndId(Long id, Long id1, Long id2);
}
