package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.in.JudgeGetParams;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.security.tournament.TournamentSecurity;
import com.heliozz10.debetter.service.tournament.JudgeService;
import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/judges")
public class JudgeController {
    private static final Set<String> PUBLIC_SORT_PROPERTIES = Set.of("id", "fullName", "checkedIn");
    private static final Set<String> PRIVATE_SORT_PROPERTIES = Set.of("email", "phoneNumber");
    private static final Set<String> ORGANIZER_SORT_PROPERTIES = Set.of(
            "id", "fullName", "checkedIn", "email", "phoneNumber"
    );

    private final JudgeService judgeService;
    private final JudgeMapper judgeMapper;
    private final TournamentSecurity tournamentSecurity;

    @GetMapping
    @PreAuthorize("@tournamentSecurity.canReadTournament(authentication, #tournamentId)")
    public PageableResult<JudgeView> getJudges(
            @PathVariable Long tournamentId,
            @Valid @ModelAttribute JudgeGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable,
            Authentication authentication
    ) {
        boolean canViewContactDetails = canViewContactDetails(authentication, tournamentId);
        validateQueryAccess(params, pageable, canViewContactDetails);

        Page<Judge> judges = judgeService.getJudges(tournamentId, params, pageable);
        return new PageableResult<>(
                canViewContactDetails
                        ? judgeMapper.toOrganizerJudgeViews(judges.getContent())
                        : judgeMapper.toPublicJudgeViews(judges.getContent()),
                judges.getTotalElements(),
                judges.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@tournamentSecurity.canReadTournament(authentication, #tournamentId)")
    public JudgeView getJudgeById(
            @PathVariable Long tournamentId,
            @PathVariable Long id,
            Authentication authentication
    ) {
        Judge judge = judgeService.getJudgeByTournamentIdAndId(tournamentId, id);
        return canViewContactDetails(authentication, tournamentId)
                ? judgeMapper.toOrganizerJudgeView(judge)
                : judgeMapper.toPublicJudgeView(judge);
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PostMapping
    public JudgeView addJudgeToTournament(@PathVariable Long tournamentId, @Validated({OnCreate.class, Default.class}) @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toOrganizerJudgeView(judgeService.addJudgeToTournament(judgeFormDto, tournamentId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{id}")
    public JudgeView updateJudge(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toOrganizerJudgeView(judgeService.updateJudge(judgeFormDto, tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping("/{id}")
    public void removeJudgeFromTournament(@PathVariable Long tournamentId, @PathVariable Long id) {
        judgeService.removeJudgeFromTournament(id, tournamentId);
    }

    private boolean canViewContactDetails(Authentication authentication, Long tournamentId) {
        return tournamentSecurity.hasResultEntryPermission(authentication, tournamentId);
    }

    private void validateQueryAccess(
            JudgeGetParams params,
            Pageable pageable,
            boolean canViewContactDetails
    ) {
        boolean requestsPrivateData = params.searchEmail() != null
                || params.phoneNumber() != null
                || pageable.getSort().stream()
                        .map(order -> order.getProperty())
                        .anyMatch(PRIVATE_SORT_PROPERTIES::contains);

        if (requestsPrivateData && !canViewContactDetails) {
            throw new AccessDeniedException("Judge contact filters and sorting require organizer access");
        }

        Set<String> allowedSortProperties = canViewContactDetails
                ? ORGANIZER_SORT_PROPERTIES
                : PUBLIC_SORT_PROPERTIES;

        pageable.getSort().stream()
                .map(order -> order.getProperty())
                .filter(property -> !allowedSortProperties.contains(property))
                .findFirst()
                .ifPresent(property -> {
                    throw new IllegalArgumentException("Unsupported judge sort property: " + property);
                });
    }
}
