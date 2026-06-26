package com.heliozz10.debetter.service.tournament;

import com.heliozz10.debetter.content.tournament.DebateFormat;
import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.tournament.TournamentParticipant;
import com.heliozz10.debetter.content.tournament.team.Club;
import com.heliozz10.debetter.content.tournament.team.Team;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.profile.ParticipantProfile;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.dto.tournament.team.in.ParticipantSelectorDto;
import com.heliozz10.debetter.dto.tournament.team.in.TeamUpdateOrganizerDto;
import com.heliozz10.debetter.mapper.tournament.TeamMapper;
import com.heliozz10.debetter.repository.tournament.TournamentParticipantRepository;
import com.heliozz10.debetter.repository.tournament.team.TeamRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.profile.ParticipantProfileRepository;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import com.heliozz10.debetter.service.CommonService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    @Mock
    private EntityManager entityManager;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private ParticipantProfileRepository participantProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Mock
    private CommonService commonService;

    @Mock
    private TournamentParticipantService tournamentParticipantService;

    @Mock
    private TournamentSecurity tournamentSecurity;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                entityManager,
                teamRepository,
                teamMapper,
                participantProfileRepository,
                userRepository,
                tournamentParticipantRepository,
                commonService,
                tournamentParticipantService,
                tournamentSecurity
        );
    }

    @Test
    void organizerUpdateCanRenameClubAndReplaceTeamMembersDirectly() {
        Team team = team(7L, DebateFormat.APF);
        ParticipantProfile keptProfile = participant(11L, 111L, "speaker1");
        ParticipantProfile removedProfile = participant(12L, 112L, "speaker2");
        ParticipantProfile addedProfile = participant(13L, 113L, "speaker3");
        TournamentParticipant keptMember = member(71L, team, keptProfile);
        TournamentParticipant removedMember = member(72L, team, removedProfile);
        team.setMembers(new ArrayList<>(List.of(keptMember, removedMember)));
        team.setCheckedIn(true);
        Club newClub = club(5L, "New Club");

        when(teamRepository.findByTournamentIdAndId(53L, 7L)).thenReturn(Optional.of(team));
        when(commonService.findOrCreateEntity(eq("New Club"), eq(Club.class), same(entityManager))).thenReturn(newClub);
        when(userRepository.findByUsername("speaker1")).thenReturn(Optional.of(keptProfile.getUser()));
        when(userRepository.findByUsername("speaker3")).thenReturn(Optional.of(addedProfile.getUser()));
        when(tournamentParticipantRepository.findByTeam_Tournament_IdAndParticipantProfile_Id(53L, 11L))
                .thenReturn(Optional.of(keptMember));
        when(tournamentParticipantRepository.findByTeam_Tournament_IdAndParticipantProfile_Id(53L, 13L))
                .thenReturn(Optional.empty());

        TeamUpdateOrganizerDto dto = new TeamUpdateOrganizerDto(
                " Renamed Team ",
                " New Club ",
                List.of(
                        new ParticipantSelectorDto(null, "speaker1"),
                        new ParticipantSelectorDto(null, "speaker3")
                )
        );

        teamService.updateTeam_Organizer(dto, 53L, 7L);

        assertEquals("Renamed Team", team.getName());
        assertEquals(newClub, team.getClub());
        assertFalse(team.getCheckedIn());
        assertTrue(team.getActive());
        assertEquals(List.of(11L, 13L), team.getMembers().stream()
                .map(member -> member.getParticipantProfile().getId())
                .toList());
        verify(tournamentParticipantRepository).delete(removedMember);
        verify(tournamentParticipantRepository).save(org.mockito.ArgumentMatchers.argThat(member ->
                member.getTeam() == team && member.getParticipantProfile() == addedProfile && member.getSpeakerScore() == 0
        ));
        verify(tournamentSecurity).removeRoleFromUser(112L, 53L, TournamentRole.VIEW);
        verify(tournamentSecurity).assignRoleToUser(113L, 53L, TournamentRole.VIEW);
        verify(teamRepository).save(team);
    }

    @Test
    void organizerUpdateRejectsDuplicateParticipants() {
        Team team = team(7L, DebateFormat.APF);
        ParticipantProfile profile = participant(11L, 111L, "speaker1");
        when(teamRepository.findByTournamentIdAndId(53L, 7L)).thenReturn(Optional.of(team));
        when(userRepository.findByUsername("speaker1")).thenReturn(Optional.of(profile.getUser()));

        TeamUpdateOrganizerDto dto = new TeamUpdateOrganizerDto(
                null,
                null,
                List.of(
                        new ParticipantSelectorDto(null, "speaker1"),
                        new ParticipantSelectorDto(null, "speaker1")
                )
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.updateTeam_Organizer(dto, 53L, 7L)
        );

        assertEquals("A participant cannot be listed twice in the same team", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    @Test
    void organizerUpdateRejectsParticipantAlreadyRegisteredForAnotherTeam() {
        Team team = team(7L, DebateFormat.APF);
        ParticipantProfile profile = participant(11L, 111L, "speaker1");
        Team otherTeam = team(8L, DebateFormat.APF);
        TournamentParticipant existingMembership = member(88L, otherTeam, profile);
        when(teamRepository.findByTournamentIdAndId(53L, 7L)).thenReturn(Optional.of(team));
        when(userRepository.findByUsername("speaker1")).thenReturn(Optional.of(profile.getUser()));
        when(tournamentParticipantRepository.findByTeam_Tournament_IdAndParticipantProfile_Id(53L, 11L))
                .thenReturn(Optional.of(existingMembership));

        TeamUpdateOrganizerDto dto = new TeamUpdateOrganizerDto(
                null,
                null,
                List.of(new ParticipantSelectorDto(null, "speaker1"))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> teamService.updateTeam_Organizer(dto, 53L, 7L)
        );

        assertEquals("Participant is already registered for another team in this tournament", exception.getMessage());
        verify(teamRepository, never()).save(team);
    }

    private static Team team(Long id, DebateFormat format) {
        Tournament tournament = new Tournament();
        tournament.setId(53L);
        tournament.setPreliminaryFormat(format);

        Team team = new Team();
        team.setId(id);
        team.setName("Team " + id);
        team.setTournament(tournament);
        team.setActive(false);
        team.setCheckedIn(false);
        team.setMembers(new ArrayList<>());
        return team;
    }

    private static Club club(Long id, String name) {
        Club club = new Club();
        club.setId(id);
        club.setName(name);
        return club;
    }

    private static ParticipantProfile participant(Long profileId, Long userId, String username) {
        User user = new User(username, "password", username + "@example.com", "First", "Last", Role.PARTICIPANT);
        user.setId(userId);

        ParticipantProfile profile = new ParticipantProfile();
        profile.setId(profileId);
        profile.setUser(user);
        user.setProfile(profile);
        return profile;
    }

    private static TournamentParticipant member(Long id, Team team, ParticipantProfile profile) {
        TournamentParticipant member = new TournamentParticipant();
        member.setId(id);
        member.setTeam(team);
        member.setParticipantProfile(profile);
        member.setSpeakerScore(0);
        return member;
    }
}
