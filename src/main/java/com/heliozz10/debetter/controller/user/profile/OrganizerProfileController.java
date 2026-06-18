package com.heliozz10.debetter.controller.user.profile;

import com.heliozz10.debetter.dto.user.profile.out.OrganizerProfileView;
import com.heliozz10.debetter.mapper.user.profile.OrganizerProfileMapper;
import com.heliozz10.debetter.service.user.profile.OrganizerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/organizer-profiles")
public class OrganizerProfileController {
    private final OrganizerProfileService organizerProfileService;
    private final OrganizerProfileMapper organizerProfileMapper;

    @GetMapping("/{id}")
    public OrganizerProfileView getOrganizerProfileById(@PathVariable Long id) {
        return organizerProfileMapper.toOrganizerProfileView(organizerProfileService.getProfileById(id));
    }
}
