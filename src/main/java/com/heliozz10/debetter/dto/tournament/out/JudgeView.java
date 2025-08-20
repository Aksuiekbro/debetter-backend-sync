package com.heliozz10.debetter.dto.tournament.out;

import com.heliozz10.debetter.dto.util.socials.out.SocialProfileView;
import lombok.Data;

import java.util.List;

@Data
public class JudgeView {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private List<SocialProfileView> socialProfiles;
    private Boolean checkedIn;
}
