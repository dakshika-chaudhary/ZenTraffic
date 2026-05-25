package com.zentraffic.auth.controller;

import com.zentraffic.auth.service.AuthService;
import com.zentraffic.common.dto.AuthResponse;
import com.zentraffic.common.dto.LoginRequest;
import com.zentraffic.common.dto.RefreshTokenRequest;
import com.zentraffic.common.dto.RegistrationOtpRequest;
import com.zentraffic.common.dto.RegistrationOtpResponse;
import com.zentraffic.common.dto.SignupRequest;
import com.zentraffic.common.dto.UserProfile;
import com.zentraffic.common.dto.VerifyRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registration/otp")
    public RegistrationOtpResponse requestRegistrationOtp(@Valid @RequestBody RegistrationOtpRequest request) {
        return authService.requestRegistrationOtp(request);
    }

    @PostMapping("/registration/verify")
    public AuthResponse verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        return authService.verifyRegistration(request);
    }

    @PostMapping("/signup")
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
    }

    @GetMapping("/profile")
    public UserProfile profile(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return authService.profile(authorization);
    }
}
