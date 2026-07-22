package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.UpdateProfileRequest;
import com.teaverse.compensation.dto.response.MatchResponse;
import com.teaverse.compensation.dto.response.UserResponse;
import com.teaverse.compensation.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(userService.me());
    }

    @PatchMapping("/me/profile")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated", userService.updateProfile(request));
    }

    @GetMapping("/matches")
    public ApiResponse<List<MatchResponse>> smartMatches() {
        return ApiResponse.ok(userService.smartMatches());
    }
}
