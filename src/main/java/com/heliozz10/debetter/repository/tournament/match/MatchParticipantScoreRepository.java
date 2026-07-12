package com.heliozz10.debetter.repository.tournament.match;

import com.heliozz10.debetter.content.tournament.match.MatchParticipantScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchParticipantScoreRepository extends JpaRepository<MatchParticipantScore, Long> {
    long countByMatchId(Long matchId);
}
