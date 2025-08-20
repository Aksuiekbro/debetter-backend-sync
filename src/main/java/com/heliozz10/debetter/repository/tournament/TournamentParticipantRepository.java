package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long>, JpaSpecificationExecutor<TournamentParticipant> {
   Optional<TournamentParticipant> findByTournamentIdAndId(Long tournamentId, Long id);
}
