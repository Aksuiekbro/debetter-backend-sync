package com.heliozz10.debetter.repository.tournament.match;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.match.DebaterMatchupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DebaterMatchupHistoryRepository extends JpaRepository<DebaterMatchupHistory, Long> {
    List<DebaterMatchupHistory> findByDebater1InAndDebater2In(Collection<TournamentParticipant> debater1s, Collection<TournamentParticipant> debater2s);
    Optional<DebaterMatchupHistory> findByDebater1AndDebater2(TournamentParticipant debater1, TournamentParticipant debater2);
}
