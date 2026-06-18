package com.heliozz10.debetter.controller.user;

import com.heliozz10.debetter.content.user.User;
import com.heliozz10.debetter.content.util.socials.SocialPlatform;
import com.heliozz10.debetter.dto.common.out.PageableResult;
import com.heliozz10.debetter.dto.user.in.UserGetParams;
import com.heliozz10.debetter.dto.user.in.UserUpdateDto;
import com.heliozz10.debetter.dto.user.out.SimpleUserView;
import com.heliozz10.debetter.dto.user.out.UserView;
import com.heliozz10.debetter.dto.util.socials.in.SocialProfileDto;
import com.heliozz10.debetter.dto.util.socials.in.SocialProfilesDto;
import com.heliozz10.debetter.mapper.user.UserMapper;
import com.heliozz10.debetter.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public PageableResult<SimpleUserView> getUsers(
            @Valid @ModelAttribute UserGetParams params,
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

    @Cacheable(value = "currentUser", key = "#authentication.principal.id")
    @GetMapping("/me")
    public UserView getMe(Authentication authentication) {
        Long id = ((User) authentication.getPrincipal()).getId();
        return userMapper.toUserView(userService.getUserById(id));
    }

    @PreAuthorize("@userSecurity.canEditUser(principal, #id)")
    @PatchMapping("/{id}")
    public UserView updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDto user) {
        return userMapper.toUserView(userService.updateUser(user, id));
    }

    @PreAuthorize("@userSecurity.canEditUser(principal, #id)")
    @PostMapping("/{id}/profile-picture")
    public void addOrUpdateProfilePicture(@PathVariable Long id, @RequestPart("image") MultipartFile file) {
        userService.addOrUpdateProfilePicture(id, file);
    }

    @PostMapping("/me/profile-picture")
    public void addOrUpdateMyProfilePicture(Authentication authentication, @RequestPart("image") MultipartFile file) {
        Long id = ((User) authentication.getPrincipal()).getId();
        userService.addOrUpdateProfilePicture(id, file);
    }

    @PreAuthorize("@userSecurity.canEditUser(principal, #id)")
    @DeleteMapping("/{id}/profile-picture")
    public void deleteProfilePicture(@PathVariable Long id) {
        userService.deleteProfilePicture(id);
    }

    @DeleteMapping("/me/profile-picture")
    public void deleteMyProfilePicture(Authentication authentication) {
        Long id = ((User) authentication.getPrincipal()).getId();
        userService.deleteProfilePicture(id);
    }

    @PreAuthorize("@userSecurity.canEditUser(principal, #id)")
    @PostMapping("/{id}/social-profiles")
    public void addOrUpdateSocialProfiles(@PathVariable Long id, @Valid @RequestBody SocialProfilesDto newProfiles) {
        userService.addOrUpdateSocialProfiles(id, newProfiles.socialProfiles());
    }

    @PostMapping("/me/social-profiles")
    public void addOrUpdateMySocialProfiles(Authentication authentication, @Valid @RequestBody SocialProfilesDto newProfiles) {
        Long id = ((User) authentication.getPrincipal()).getId();
        userService.addOrUpdateSocialProfiles(id, newProfiles.socialProfiles());
    }

    @PreAuthorize("@userSecurity.canEditUser(principal, #id)")
    @DeleteMapping("/{id}/social-profiles")
    public void removeAllSocialProfiles(@PathVariable Long id, @RequestParam(required = false) List<SocialPlatform> platforms) {
        if(platforms != null) {
            userService.removeSocialProfiles(id, platforms);
        } else {
            userService.removeAllSocialProfiles(id);
        }
    }

    @DeleteMapping("/me/social-profiles")
    public void removeAllMySocialProfiles(Authentication authentication, @RequestParam(required = false) List<SocialPlatform> platforms) {
        Long id = ((User) authentication.getPrincipal()).getId();
        if(platforms != null) {
            userService.removeSocialProfiles(id, platforms);
        } else {
            userService.removeAllSocialProfiles(id);
        }
    }
}
