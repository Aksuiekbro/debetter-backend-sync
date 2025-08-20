package com.heliozz10.debetter.dto.user.out;

import com.heliozz10.debetter.dto.user.profile.out.ProfileView;
import com.heliozz10.debetter.dto.util.socials.out.SocialProfileView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserView extends SimpleUserView {
    private String email;
    private ProfileView profile;
    private List<SocialProfileView> socialProfiles;
    private LocalDateTime createdAt;
}
