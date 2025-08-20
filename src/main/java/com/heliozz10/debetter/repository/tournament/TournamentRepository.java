package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.round.Round;
import com.heliozz10.debetter.content.tournament.round.RoundGroupType;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.projection.TournamentCheckResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {
    List<Tournament> findByStartDateBetween(LocalDateTime start, LocalDateTime end);
    List<Tournament> findByEndDateBetween(LocalDateTime start, LocalDateTime end);
    List<Tournament> findByRegistrationDeadlineBetween(LocalDateTime start, LocalDateTime end);

    List<Tournament> findByLeague(TournamentLeague league);

    List<Tournament> findByMainOrganizerId(Long mainOrganizerId);

    @Query("SELECT r.roundGroup.tournament FROM Round r WHERE r.id = :roundId")
    Tournament findByRoundId(@Param("roundId") Long roundId);

    @Query("""
        SELECT u
        FROM Tournament t
        LEFT JOIN t.mainOrganizer o
        LEFT JOIN User u ON u.profile = o
        WHERE t.id = :tournamentId
    """)
    Optional<User> findMainOrganizerByTournamentId(Long tournamentId);

    @Query("""
        SELECT DISTINCT u
        FROM Tournament t
        LEFT JOIN t.organizers o
        LEFT JOIN User u ON u.profile = o OR u.profile = t.mainOrganizer
        WHERE t.id = :tournamentId
    """)
    List<User> findOrganizersByTournamentId(@Param("tournamentId") Long tournamentId);

    @Modifying
    @Query("UPDATE Tournament t SET t.finished = true WHERE t.id = :id")
    void setFinishedById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Tournament t SET t.finished = false WHERE t.id = :id")
    void setNotFinishedById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Tournament t SET t.disabled = true WHERE t.id = :id")
    void disableById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Tournament t SET t.disabled = false WHERE t.id = :id")
    void enableById(@Param("id") Long id);

    @Query(value = """
        SELECT
          SUM(CASE WHEN t.checked_in = false THEN 1 ELSE 0 END) AS unchecked_in,
          (SELECT COUNT(*) FROM judge j WHERE j.tournament_id = :tournamentId) AS judge_count,
          COUNT(*) AS team_count
        FROM team t
        WHERE t.tournament_id = :tournamentId
    """, nativeQuery = true)
    TournamentCheckResult checkTournament(@Param("tournamentId") Long tournamentId);
    

    @Modifying
    @Query("UPDATE Tournament t SET t.started = true WHERE t.id = :tournamentId")
    void startTournament(@Param("tournamentId") Long tournamentId);

    @Modifying
    @Query("""
        UPDATE Round r
        SET r.teams = (SELECT t FROM Team t WHERE t.tournament.id = :tournamentId)
        WHERE r.id = :firstRoundId
    """)
    void setTeamsOfFirstRound(@Param("firstRoundId") Long firstRoundId, @Param("tournamentId") Long tournamentId);

    @Query("""
        SELECT r FROM Round r
        WHERE r.roundGroup.tournament.id = :tournamentId
        AND r.roundGroup.type = :roundGroupType
        AND r.roundNumber = :roundNumber
    """)
    Round findRound(@Param("tournamentId") Long tournamentId, @Param("roundGroupType") RoundGroupType roundGroupType, @Param("roundNumber") Integer roundNumber);

    @Transactional
    @Modifying
    @Query("UPDATE Team t SET t.tournament = NULL WHERE t.id = :teamId AND t.tournament.id = :tournamentId")
    void removeTeamFromTournament(@Param("teamId") Long teamId, @Param("tournamentId") Long tournamentId);
}
