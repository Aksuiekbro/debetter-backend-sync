package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.TournamentMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentMapRepository extends JpaRepository<TournamentMap, Long> {
    Optional<TournamentMap> findByTournamentId(Long tournamentId);

    boolean existsByTournamentId(Long tournamentId);
}
