package com.heliozz10.debetter.security.tournament;

import com.heliozz10.debetter.content.tournament.Tournament;
import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.user.role.TournamentRole;
import com.heliozz10.debetter.content.user.role.UserTournamentKey;
import com.heliozz10.debetter.content.user.role.UserTournamentRole;
import com.heliozz10.debetter.repository.tournament.TournamentRepository;
import com.heliozz10.debetter.repository.user.UserRepository;
import com.heliozz10.debetter.repository.user.UserTournamentRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Set;

@RequiredArgsConstructor
@Component
public class TournamentSecurity {
    private final TournamentRepository tournamentRepository;

    private final UserRepository userRepository;
    private final UserTournamentRoleRepository userTournamentRoleRepository;

    @CacheEvict(value = "userTournamentPermissions",
            key = "#userId + ':' + #tournamentId")
    @Transactional
    public void assignRoleToUser(Long userId, Long tournamentId, TournamentRole role) {
        User user = userRepository.getReferenceById(userId);
        Tournament tournament = tournamentRepository.getReferenceById(tournamentId);

        UserTournamentKey key = new UserTournamentKey(userId, tournamentId);

        boolean exists = userTournamentRoleRepository.existsById(key);
        if (exists) {
            throw new IllegalStateException("User already has a role for this tournament");
        }

        UserTournamentRole utr = new UserTournamentRole();
        utr.setId(key);
        utr.setUser(user);
        utr.setTournament(tournament);
        utr.setRole(role);

        userTournamentRoleRepository.save(utr);
    }

    @CacheEvict(value = "userTournamentPermissions",
            key = "#userId + ':' + #tournamentId")
    @Transactional
    public void removeRoleFromUser(Long userId, Long tournamentId, TournamentRole role) {
        userTournamentRoleRepository.deleteById_UserIdAndId_TournamentIdAndRole(userId, tournamentId, role);
    }

    @Cacheable(value = "userTournamentPermissions",
            key = "#userId + ':' + #tournamentId")
    public Set<TournamentRole> getUserRolesForTournament(Long userId, Long tournamentId) {
        return userTournamentRoleRepository.findRolesByUserIdAndTournamentId(userId, tournamentId);
    }

    public boolean hasViewPermission(UserDetails principal, Long tournamentId) {
        Long userId = ((User) principal).getId();
        Set<TournamentRole> roles = getUserRolesForTournament(userId, tournamentId);
        return roles.contains(TournamentRole.VIEW) || roles.contains(TournamentRole.EDIT) || roles.contains(TournamentRole.FULL);
    }

    public boolean hasEditPermission(UserDetails principal, Long tournamentId) {
        Long userId = ((User) principal).getId();
        Set<TournamentRole> roles = getUserRolesForTournament(userId, tournamentId);
        return roles.contains(TournamentRole.EDIT) || roles.contains(TournamentRole.FULL);
    }

    public boolean hasFullPermission(User principal, Long tournamentId) {
        Long userId = ((User) principal).getId();
        Set<TournamentRole> roles = getUserRolesForTournament(userId, tournamentId);
        return roles.contains(TournamentRole.FULL);
    }

    public boolean hasRoundViewPermission(UserDetails principal, Long roundId) {
        Long userId = ((User) principal).getId();
        return userTournamentRoleRepository.existsViewPermissionForRound(userId, roundId);
    }

    public boolean hasResultEntryPermission(Authentication authentication, Long tournamentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }

        return user.getRole() == Role.ORGANIZER && hasEditPermission(user, tournamentId);
    }

    /**
     * Visible tournaments are public. A hidden tournament can only be read by
     * users with EDIT or FULL membership; VIEW intentionally does not bypass a
     * state presented as hidden from participants.
     *
     * Missing tournaments are allowed through so controllers and services keep
     * their existing not-found behavior instead of turning a 404 into a 403.
     */
    public boolean canReadTournament(Authentication authentication, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament == null || !Boolean.TRUE.equals(tournament.getDisabled())) {
            return true;
        }

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return false;
        }

        Set<TournamentRole> roles = getUserRolesForTournament(user.getId(), tournamentId);
        return roles.contains(TournamentRole.EDIT) || roles.contains(TournamentRole.FULL);
    }
}
