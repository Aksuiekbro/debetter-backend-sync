package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.tournament.in.TournamentParticipantGetParams;
import com.heliozz10.debetter.repository.specification.tournament.TournamentParticipantSpecification;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TournamentParticipantService {
    private final EntityManager entityManager;

    private final TournamentParticipantRepository tournamentParticipantRepository;

    @Transactional(readOnly = true)
    public Page<TournamentParticipant> getParticipants(Long tournamentId, TournamentParticipantGetParams params, Pageable pageable) {
        //TODO: validate tournament id exists in controller
        Specification<TournamentParticipant> spec = TournamentParticipantSpecification.filterBy(tournamentId, params, entityManager);
        return tournamentParticipantRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public TournamentParticipant getParticipantByTournamentIdAndId(Long tournamentId, Long id) {
        return tournamentParticipantRepository.findByTournamentIdAndId(tournamentId, id)
                .orElseThrow(() -> new EntityNotFoundException("Participant not found"));
    }
}
