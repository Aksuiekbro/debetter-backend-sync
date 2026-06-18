package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.dto.tournament.in.TournamentParticipantGetParams;
import com.heliozz10.debetter.dto.tournament.out.SimpleTournamentParticipantView;
import com.heliozz10.debetter.dto.tournament.out.TournamentParticipantView;
import com.heliozz10.debetter.mapper.tournament.TournamentParticipantMapper;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.repository.specification.tournament.TournamentParticipantSpecification;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TournamentParticipantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TournamentParticipantService.class);

    private final EntityManager entityManager;

    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final TournamentParticipantMapper tournamentParticipantMapper;

    private final UserMapper userMapper;

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

    public SimpleTournamentParticipantView toSimpleTournamentParticipantView(TournamentParticipant tournamentParticipant) {
        SimpleTournamentParticipantView view = tournamentParticipantMapper.toSimpleTournamentParticipantView(tournamentParticipant);
        view.setUser(userMapper.toSimpleUserView(tournamentParticipant.getParticipantProfile().getUser()));
        return view;
    }

    public TournamentParticipantView toTournamentParticipantView(TournamentParticipant tournamentParticipant) {
        TournamentParticipantView view = tournamentParticipantMapper.toTournamentParticipantView(tournamentParticipant);
        view.setUser(userMapper.toSimpleUserView(tournamentParticipant.getParticipantProfile().getUser()));
        LOGGER.info(view.toString());
        return view;
    }
}
