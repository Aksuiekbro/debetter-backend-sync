package com.heliozz10.debetter.security.tournament;

import com.heliozz10.debetter.repository.tournament.match.MatchRepository;
import com.heliozz10.debetter.security.tournament.match.MatchUpdateRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@RequiredArgsConstructor
@Component
public class TournamentPermissionEvaluator implements PermissionEvaluator {
    private final MatchRepository matchRepository;
    private final MatchUpdateRequestContext context;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if("TOURNAMENT".equalsIgnoreCase(targetType) && "UPDATE_MATCHES".equalsIgnoreCase(permission.toString())) {
            Long tournamentId = (Long) targetId;

            if(authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("TOURNAMENT_" + tournamentId))) {
                return false;
            }
            List<Long> matchIds = context.getMatchIds();
            if(matchIds != null) {
                long count = matchRepository.countMatchesInTournament(tournamentId, matchIds);
                return count == matchIds.size();
            }
        }
        return false;
    }
}