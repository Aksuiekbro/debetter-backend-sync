package com.heliozz10.debetter.security.user;

import com.heliozz10.debetter.content.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class UserSecurity {
    public boolean canEditUser(UserDetails principal, Long targetUserId) {
        User user = (User) principal;
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority -> authority.equals("ADMIN")) || user.getId().equals(targetUserId);
    }
}
