package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JudgeRepository extends JpaRepository<Judge, Long>, JpaSpecificationExecutor<Judge> {
    List<Judge> findByTournamentId(Long tournamentId);

    Optional<Judge> findByTournamentIdAndId(Long tournamentId, Long id);
}
