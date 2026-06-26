package com.heliozz10.debetter.repository.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long>, JpaSpecificationExecutor<TournamentParticipant> {
    @EntityGraph(value = "TournamentParticipant.forSimpleView", type = EntityGraph.EntityGraphType.LOAD)
    @Override
    Page<TournamentParticipant> findAll(Specification<TournamentParticipant> spec, Pageable pageable);

    @Query("SELECT tp FROM TournamentParticipant tp WHERE tp.team.tournament.id = :tournamentId AND tp.id = :id")
    @EntityGraph(value = "TournamentParticipant.forView", type = EntityGraph.EntityGraphType.LOAD)
    Optional<TournamentParticipant> findByTournamentIdAndId(Long tournamentId, Long id);

    Optional<TournamentParticipant> findByTeam_Tournament_IdAndParticipantProfile_Id(Long tournamentId, Long participantProfileId);

    boolean existsByTeam_Tournament_IdAndParticipantProfile_Id(Long tournamentId, Long participantProfileId);
}
