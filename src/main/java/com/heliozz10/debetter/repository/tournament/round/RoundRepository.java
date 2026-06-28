package com.heliozz10.debetter.repository.tournament.round;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.round.Round;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(value = "Round.withTeams", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Round> findWithTeamsByRoundGroup_IdAndRoundNumber(Long id, Integer roundNumber);

    @Query("SELECT r.debaters FROM Round r WHERE r.id = :roundId")
    List<TournamentParticipant> findDebatersByRoundId(Long roundId);

    @Modifying
    @Query("UPDATE Round r SET r.customFormat = :format WHERE r.id = :roundId")
    void changeRoundFormat(@Param("roundId") Long roundId, @Param("format") DebateFormat format);

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 AND SUM(CASE WHEN m.completed = false THEN 1 ELSE 0 END) = 0
                    THEN true ELSE false END
        FROM Match m
        WHERE m.round = :round
    """)
    boolean areAllMatchesCompleted(@Param("round") Round round);

    Optional<Round> findByRoundGroup_Tournament_IdAndId(Long id, Long id1);

    List<Round> findByRoundGroup_Tournament_IdAndRoundGroup_Id(Long id, Long id1);

    @EntityGraph(value = "Round.forView", type = EntityGraph.EntityGraphType.LOAD)
    Optional<Round> findByRoundGroup_Tournament_IdAndRoundGroup_IdAndId(Long id, Long id1, Long id2);

    @EntityGraph(attributePaths = {"roundGroup"})
    @Query("""
        SELECT r FROM Round r
        WHERE r.roundGroup.tournament.id = :tournamentId
          AND r.roundGroup.id = :roundGroupId
          AND r.id = :roundId
    """)
    Optional<Round> findWithPairingStateByTournamentAndRoundGroupAndId(
            @Param("tournamentId") Long tournamentId,
            @Param("roundGroupId") Long roundGroupId,
            @Param("roundId") Long roundId
    );
}
