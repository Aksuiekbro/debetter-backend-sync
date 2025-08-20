package com.heliozz10.debetter.repository.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.round.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {
    List<Round> findByRoundGroupId(Long roundGroupId);

    @Query("""
        SELECT r FROM Round r
        WHERE r.roundGroup.tournament.id = :tournamentId
    """)
    List<Round> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT r.roundGroup.tournament FROM Round r WHERE r.id = :roundId")
    Tournament findTournamentByRoundId(@Param("roundId") Long roundId);

    Optional<Round> findByRoundGroup_IdAndRoundNumber(Long id, Integer roundNumber);

    @Modifying
    @Query("UPDATE Round r SET r.customFormat = :format WHERE r.id = :roundId")
    void changeRoundFormat(@Param("roundId") Long roundId, @Param("format") DebateFormat format);

    @Query("""
        SELECT CASE WHEN COUNT(m) = 0 THEN true ELSE false END
        FROM Match m
        WHERE m.round = :round
          AND m.completed = false
    """)
    boolean areAllMatchesCompleted(@Param("round") Round round);

    @Modifying
    @Query(value = "SELECT assign_judges_for_round(:roundId)", nativeQuery = true)
    void assignJudgesForRound(@Param("roundId") Long roundId);
}
