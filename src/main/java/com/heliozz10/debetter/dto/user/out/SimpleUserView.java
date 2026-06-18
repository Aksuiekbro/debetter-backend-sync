package com.heliozz10.debetter.dto.user.out;

import com.heliozz10.debetter.content.user.Role;
import com.heliozz10.debetter.dto.util.media.out.UrlView;
import lombok.Data;

import java.io.Serializable;

@Data
public class SimpleUserView implements Serializable {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private UrlView imageUrl;
    private Role role;
}
