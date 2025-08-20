package com.heliozz10.debetter.security.user;

import com.heliozz10.debetter.content.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserSecurity {
    public boolean canEditUser(User user, Long targetUserId) {
        return true;
    }
}
