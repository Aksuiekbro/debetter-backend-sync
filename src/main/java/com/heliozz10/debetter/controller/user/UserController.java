package com.heliozz10.debetter.controller.user;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.user.in.UserGetParams;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public PageableResult<SimpleUserView> getUsers(
            @ModelAttribute UserGetParams params,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<User> users = userService.getUsers(params, pageable);
        return new PageableResult<>(
                userMapper.toSimpleUserViews(users.getContent()),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public UserView getUserById(@PathVariable Long id) {
        return userMapper.toUserView(userService.getUserById(id));
    }

    @GetMapping("/me")
    public UserView getMe(Authentication authentication) {
        Long id = ((User) authentication.getPrincipal()).getId();
        return userMapper.toUserView(userService.getUserById(id));
    }

    @PatchMapping("/{id}")
    public UserView updateUser(@PathVariable Long id, @RequestBody UserUpdateDto user) {
        return userMapper.toUserView(userService.updateUser(user, id));
    }
}
