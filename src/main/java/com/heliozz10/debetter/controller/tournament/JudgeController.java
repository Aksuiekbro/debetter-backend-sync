package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.in.JudgeGetParams;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.service.tournament.JudgeService;
import com.heliozz10.debetter.validation.OnCreate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tournaments/{tournamentId}/judges")
public class JudgeController {
    private final JudgeService judgeService;
    private final JudgeMapper judgeMapper;

    @GetMapping
    public PageableResult<JudgeView> getJudges(
            @PathVariable Long tournamentId,
            @Valid @ModelAttribute JudgeGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Judge> judges = judgeService.getJudges(tournamentId, params, pageable);
        return new PageableResult<>(
                judgeMapper.toJudgeViews(judges.getContent()),
                judges.getTotalElements(),
                judges.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public JudgeView getJudgeById(@PathVariable Long tournamentId, @PathVariable Long id) {
        return judgeMapper.toJudgeView(judgeService.getJudgeByTournamentIdAndId(tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PostMapping
    public JudgeView addJudgeToTournament(@PathVariable Long tournamentId, @Validated(OnCreate.class) @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toJudgeView(judgeService.addJudgeToTournament(judgeFormDto, tournamentId));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @PatchMapping("/{id}")
    public JudgeView updateJudge(@PathVariable Long tournamentId, @PathVariable Long id, @Valid @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toJudgeView(judgeService.updateJudge(judgeFormDto, tournamentId, id));
    }

    @PreAuthorize("principal.role.name() == 'ORGANIZER' and @tournamentSecurity.hasEditPermission(principal, #tournamentId)")
    @DeleteMapping("/{id}")
    public void removeJudgeFromTournament(@PathVariable Long tournamentId, @PathVariable Long id) {
        judgeService.removeJudgeFromTournament(id, tournamentId);
    }
}
