package com.heliozz10.debetter.dto.user.profile.out;

import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import lombok.Data;

@Data
public abstract class ProfileView {
    private Long id;
    private SimpleUserView user;
}
