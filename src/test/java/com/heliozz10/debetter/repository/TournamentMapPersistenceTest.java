package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.tournament.TournamentMap;
import com.heliozz10.debetter.content.util.media.Url;
import com.heliozz10.debetter.repository.tournament.TournamentMapRepository;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.util.media.UrlRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tournament_map_persistence_test;DB_CLOSE_DELAY=0;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.search.backend.directory.root=target/test-lucene-indexes/tournament-map-persistence"
})
@Transactional
class TournamentMapPersistenceTest {
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TournamentMapRepository tournamentMapRepository;
    @Autowired
    private UrlRepository urlRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void databaseAllowsOnlyOneMapPerTournament() {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        tournamentMapRepository.saveAndFlush(tournamentMap(tournament, "first.png"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> tournamentMapRepository.saveAndFlush(tournamentMap(tournament, "second.png"))
        );
    }

    @Test
    void replacingMapImageOrphanRemovesThePreviousUrlRow() {
        Tournament tournament = tournamentRepository.saveAndFlush(tournament());
        TournamentMap tournamentMap = tournamentMapRepository.saveAndFlush(
                tournamentMap(tournament, "original.png")
        );
        Long oldImageId = tournamentMap.getImageUrl().getId();

        tournamentMap.setImageUrl(imageUrl("/uploads/images/tournament-maps/replacement.png"));
        tournamentMapRepository.saveAndFlush(tournamentMap);
        entityManager.clear();

        assertFalse(urlRepository.existsById(oldImageId));
        assertTrue(tournamentMapRepository.findByTournamentId(tournament.getId()).isPresent());
    }

    private static TournamentMap tournamentMap(Tournament tournament, String filename) {
        TournamentMap tournamentMap = new TournamentMap();
        tournamentMap.setTitle("Venue map");
        tournamentMap.setDescription("Rooms and entrances");
        tournamentMap.setTournament(tournament);
        tournamentMap.setImageUrl(imageUrl("/uploads/images/tournament-maps/" + filename));
        return tournamentMap;
    }

    private static Url imageUrl(String value) {
        Url url = new Url();
        url.setUrl(value);
        return url;
    }

    private static Tournament tournament() {
        Tournament tournament = new Tournament();
        tournament.setName("Tournament map persistence");
        tournament.setDescription("Persistence fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(32);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(false);
        return tournament;
    }
}
