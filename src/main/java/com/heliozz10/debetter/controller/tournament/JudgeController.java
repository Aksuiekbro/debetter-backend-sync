package com.heliozz10.debetter.controller.tournament;

import com.heliozz10.debetter.content.tournament.Judge;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.tournament.in.JudgeFormDto;
import com.heliozz10.debetter.dto.tournament.in.JudgeGetParams;
import com.heliozz10.debetter.dto.tournament.out.JudgeView;
import com.heliozz10.debetter.mapper.tournament.JudgeMapper;
import com.heliozz10.debetter.service.tournament.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
            @ModelAttribute JudgeGetParams params,
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

    @PostMapping
    public JudgeView addJudgeToTournament(@PathVariable Long tournamentId, @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toJudgeView(judgeService.addJudgeToTournament(judgeFormDto, tournamentId));
    }

    @PatchMapping("/{id}")
    public JudgeView updateJudge(@PathVariable Long tournamentId, @PathVariable Long id, @RequestBody JudgeFormDto judgeFormDto) {
        return judgeMapper.toJudgeView(judgeService.updateJudge(judgeFormDto, tournamentId, id));
    }

    @DeleteMapping("/{id}")
    public void removeJudgeFromTournament(@PathVariable Long tournamentId, @PathVariable Long id) {
        judgeService.removeJudgeFromTournament(id, tournamentId);
    }
}
