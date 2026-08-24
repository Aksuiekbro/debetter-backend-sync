package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.dto.tournament.in.TournamentGetParams;
import com.heliozz10.debetter.repository.specification.tournament.TournamentMembershipSpecification;
import com.heliozz10.debetter.repository.specification.tournament.TournamentSpecification;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class MyTournamentService {
    private final EntityManager entityManager;
    private final TournamentRepository tournamentRepository;

    @Transactional(readOnly = true)
    public Page<Tournament> getMyTournaments(
            Long userId,
            TournamentGetParams params,
            Pageable pageable
    ) {
        Specification<Tournament> specification = TournamentSpecification
                .baseFilters(params, entityManager)
                .and(TournamentMembershipSpecification.visibleMembershipsFor(userId));

        return tournamentRepository.findAll(specification, pageable);
    }
}
