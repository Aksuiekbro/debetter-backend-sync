package com.heliozz10.debetter.dto.user.out;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.content.util.media.Url;
import lombok.Data;

@Data
public class SimpleUserView {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private Url imageUrl;
    private Role role;
}
