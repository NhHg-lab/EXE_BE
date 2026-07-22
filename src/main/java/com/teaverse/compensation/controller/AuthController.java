package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.SignInRequest;
import com.teaverse.compensation.dto.request.SignUpRequest;
import com.teaverse.compensation.dto.response.AuthResponse;
import com.teaverse.compensation.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/signup", "/sign-up"})
    public ApiResponse<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.ok("Account created", authService.signUp(request));
    }

    @PostMapping({"/signin", "/sign-in"})
    public ApiResponse<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        return ApiResponse.ok("Signed in", authService.signIn(request));
    }
}
