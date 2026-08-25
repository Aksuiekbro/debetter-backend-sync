package com.heliozz10.debetter.repository;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentLeague;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.OrganizerProfile;
import com.heliozz10.debetter.content.util.request.OrganizerInvitation;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.OrganizerProfileRepository;
import com.heliozz10.debetter.repository.util.request.OrganizerInvitationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.search.enabled=false")
@Import(OrganizerInvitationVisibilityPersistenceTest.CacheTestConfiguration.class)
class OrganizerInvitationVisibilityPersistenceTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizerProfileRepository organizerProfileRepository;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private OrganizerInvitationRepository organizerInvitationRepository;

    @Test
    void sentAndReceivedPagesExcludeDisabledTournamentInvitations() {
        OrganizerProfile inviter = organizer("inviter");
        OrganizerProfile invitee = organizer("invitee");
        Tournament visibleTournament = tournamentRepository.saveAndFlush(tournament(inviter, false));
        Tournament hiddenTournament = tournamentRepository.saveAndFlush(tournament(inviter, true));
        OrganizerInvitation visibleInvitation = organizerInvitationRepository.saveAndFlush(
                invitation(inviter, invitee, visibleTournament)
        );
        organizerInvitationRepository.saveAndFlush(invitation(inviter, invitee, hiddenTournament));

        var sentPage = organizerInvitationRepository.findByInviterId(inviter.getId(), PageRequest.of(0, 10));
        var receivedPage = organizerInvitationRepository.findByInviteeId(invitee.getId(), PageRequest.of(0, 10));
        List<Long> sentIds = sentPage.map(OrganizerInvitation::getId).getContent();
        List<Long> receivedIds = receivedPage.map(OrganizerInvitation::getId).getContent();

        assertEquals(List.of(visibleInvitation.getId()), sentIds);
        assertEquals(List.of(visibleInvitation.getId()), receivedIds);
        assertEquals(1, sentPage.getTotalElements());
        assertEquals(1, receivedPage.getTotalElements());
    }

    private OrganizerProfile organizer(String prefix) {
        String username = prefix + "-" + UUID.randomUUID();
        User user = userRepository.saveAndFlush(new User(
                username,
                UUID.randomUUID().toString(),
                username + "@example.invalid",
                "Test",
                "Organizer",
                Role.ORGANIZER
        ));
        OrganizerProfile profile = new OrganizerProfile();
        profile.setUser(user);
        profile = organizerProfileRepository.saveAndFlush(profile);
        user.setProfile(profile);
        return profile;
    }

    private static OrganizerInvitation invitation(
            OrganizerProfile inviter,
            OrganizerProfile invitee,
            Tournament tournament
    ) {
        OrganizerInvitation invitation = new OrganizerInvitation();
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setTournament(tournament);
        invitation.setTimestamp(LocalDateTime.now());
        invitation.setAccepted(false);
        return invitation;
    }

    private static Tournament tournament(OrganizerProfile organizer, boolean disabled) {
        Tournament tournament = new Tournament();
        tournament.setName(disabled ? "Hidden Invitational" : "Visible Invitational");
        tournament.setDescription("Organizer invitation visibility fixture");
        tournament.setStartDate(LocalDateTime.now().plusDays(7));
        tournament.setEndDate(LocalDateTime.now().plusDays(8));
        tournament.setRegistrationDeadline(LocalDateTime.now().plusDays(6));
        tournament.setLocation("Almaty");
        tournament.setLeague(TournamentLeague.SCHOOL);
        tournament.setTeamLimit(8);
        tournament.setPreliminaryFormat(DebateFormat.APF);
        tournament.setTeamEliminationFormat(DebateFormat.APF);
        tournament.setMainOrganizer(organizer);
        tournament.setOrganizers(List.of(organizer));
        tournament.setStarted(false);
        tournament.setFinished(false);
        tournament.setDisabled(disabled);
        return tournament;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CacheTestConfiguration {
        @Bean
        CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("userTournamentPermissions");
        }
    }
}
