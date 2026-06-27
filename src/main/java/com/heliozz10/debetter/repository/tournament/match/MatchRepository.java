package com.heliozz10.debetter.repository.tournament.match;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.match.Match;
import com.heliozz10.debetter.content.tournament.round.RoundGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @EntityGraph(value = "Match.forView", type = EntityGraph.EntityGraphType.LOAD)
    Page<Match> findByRoundId(Long roundId, Pageable pageable);

    @EntityGraph(value = "Match.forView", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
        SELECT m FROM Match m
        WHERE m.id = :matchId
          AND m.round.id = :roundId
          AND m.round.roundGroup.id = :roundGroupId
          AND m.round.roundGroup.tournament.id = :tournamentId
    """)
    Optional<Match> findByTournamentRoundGroupRoundAndId(
            @Param("tournamentId") Long tournamentId,
            @Param("roundGroupId") Long roundGroupId,
            @Param("roundId") Long roundId,
            @Param("matchId") Long matchId
    );

    List<Match> findByRoundIdAndJudgeIsNullOrderByIdAsc(Long roundId);

    @Query("SELECT m FROM Match m WHERE m.team1 = :teamId OR m.team2 = :teamId OR m.team3 = :teamId OR m.team4 = :teamId")
    List<Match> findByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT m FROM Match m WHERE m.debater1 = :debaterId OR m.debater2 = :debaterId")
    List<Match> findByDebaterId(@Param("debaterId") Long debaterId);

    List<Match> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT m FROM Match m
        WHERE m.round.roundGroup.tournament.id = :tournamentId
    """)
    List<Match> findByTournamentId(@Param("tournamentId") Long tournamentId);

    @Query("SELECT m.round.roundGroup.tournament FROM Match m WHERE m.id = :matchId")
    Tournament findTournamentByMatchId(@Param("matchId") Long matchId);

    @Query("SELECT m.round.roundGroup FROM Match m WHERE m.id = :matchId")
    RoundGroup findRoundGroupByMatchId(@Param("matchId") Long matchId);

    @Transactional
    @Query(value = "SELECT update_match_scores_bulk(CAST(:resultsJson AS jsonb))", nativeQuery = true)
    Object updateMatchScoresBulk(@Param("resultsJson") String resultsJson);

    /**
     * Needed for ownership check
     * @param tournamentId
     * @param matchIds
     * @return
     */
    @Query("""
        SELECT COUNT(m) 
        FROM Match m
        JOIN m.round r
        JOIN r.roundGroup rg
        JOIN rg.tournament t
        WHERE t.id = :tournamentId 
          AND m.id IN :matchIds
    """)
    long countMatchesInTournament(@Param("tournamentId") Long tournamentId,
                                  @Param("matchIds") List<Long> matchIds);
}
