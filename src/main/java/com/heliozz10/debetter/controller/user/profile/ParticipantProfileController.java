package com.heliozz10.debetter.controller.user.profile;

import com.heliozz10.debetter.content.user.profile.City;
import com.heliozz10.debetter.content.user.profile.institution.Institution;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.user.profile.out.ParticipantProfileView;
import com.heliozz10.debetter.mapper.user.profile.ParticipantProfileMapper;
import com.heliozz10.debetter.service.user.profile.ParticipantProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ParticipantProfileController {
    private final ParticipantProfileService participantProfileService;
    private final ParticipantProfileMapper participantProfileMapper;

    @GetMapping("/participant-profiles/{id}")
    public ParticipantProfileView getParticipantProfileById(@PathVariable Long id) {
        return participantProfileMapper.toParticipantProfileView(participantProfileService.getProfileById(id));
    }

    @GetMapping("/cities")
    public PageableResult<City> getCities(
            @RequestParam String searchName,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<City> cities = participantProfileService.getCities(searchName, pageable);
        return new PageableResult<>(
                cities.getContent(),
                cities.getTotalElements(),
                cities.getTotalPages()
        );
    }

    @GetMapping("/institutions")
    public PageableResult<Institution> getInstitutions(
            @RequestParam String searchName,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<Institution> institutions = participantProfileService.getInstitutions(searchName, pageable);
        return new PageableResult<>(
                institutions.getContent(),
                institutions.getTotalElements(),
                institutions.getTotalPages()
        );
    }
}
