package com.zentraffic.auth.service;

import com.zentraffic.auth.entity.Role;
import com.zentraffic.auth.entity.PendingRegistration;
import com.zentraffic.auth.entity.RefreshToken;
import com.zentraffic.auth.entity.User;
import com.zentraffic.auth.repository.PendingRegistrationRepository;
import com.zentraffic.auth.repository.RefreshTokenRepository;
import com.zentraffic.auth.repository.UserRepository;
import com.zentraffic.common.dto.AuthResponse;
import com.zentraffic.common.dto.LoginRequest;
import com.zentraffic.common.dto.RefreshTokenRequest;
import com.zentraffic.common.dto.RegistrationOtpRequest;
import com.zentraffic.common.dto.RegistrationOtpResponse;
import com.zentraffic.common.dto.SignupRequest;
import com.zentraffic.common.dto.UserProfile;
import com.zentraffic.common.dto.VerifyRegistrationRequest;
import com.zentraffic.common.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {
    private static final int OTP_TTL_SECONDS = 600;
    private static final long REFRESH_TTL_SECONDS = 15L * 24L * 60L * 60L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PendingRegistrationRepository pendingRegistrations;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long ttlSeconds;

    public AuthService(UserRepository users,
                       PendingRegistrationRepository pendingRegistrations,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder,
                       @Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.ttl-seconds}") long ttlSeconds) {
        this.users = users;
        this.pendingRegistrations = pendingRegistrations;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = new JwtService(secret, ttlSeconds);
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public RegistrationOtpResponse requestRegistrationOtp(RegistrationOtpRequest request) {
        String email = request.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        pendingRegistrations.deleteByEmail(email);
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        PendingRegistration pending = new PendingRegistration();
        pending.setName(request.name());
        pending.setEmail(email);
        pending.setPasswordHash(passwordEncoder.encode(request.password()));
        pending.setRole(parseRole(request.role()));
        pending.setOtpHash(passwordEncoder.encode(otp));
        pending.setExpiresAt(Instant.now().plusSeconds(OTP_TTL_SECONDS));
        pendingRegistrations.save(pending);
        System.out.println("Registration OTP for " + email + ": " + otp);
        return new RegistrationOtpResponse("OTP sent. For local development, use the devOtp value.", email, OTP_TTL_SECONDS, otp);
    }

    @Transactional
    public AuthResponse verifyRegistration(VerifyRegistrationRequest request) {
        String email = request.email().toLowerCase();
        PendingRegistration pending = pendingRegistrations.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("No pending registration found"));
        if (pending.getExpiresAt().isBefore(Instant.now()) || !passwordEncoder.matches(request.otp(), pending.getOtpHash())) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }
        if (users.existsByEmail(email)) {
            pendingRegistrations.delete(pending);
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setName(pending.getName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPasswordHash());
        user.setRole(pending.getRole());
        users.save(user);
        pendingRegistrations.delete(pending);
        return response(user);
    }

    public AuthResponse signup(SignupRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(parseRole(request.role()));
        users.save(user);
        return response(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return response(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken token = refreshTokens.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        token.setRevoked(true);
        refreshTokens.save(token);
        return response(token.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokens.findByToken(request.refreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokens.save(token);
        });
    }

    public UserProfile profile(String authorization) {
        String token = authorization.replace("Bearer ", "");
        var claims = jwtService.validate(token);
        Long userId = Long.valueOf(String.valueOf(claims.get("userId")));
        return users.findById(userId).map(this::profile).orElseThrow();
    }

    private AuthResponse response(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(REFRESH_TTL_SECONDS));
        refreshTokens.save(refreshToken);
        return new AuthResponse(
                jwtService.generate(user.getId(), user.getEmail(), user.getRole().name()),
                refreshToken.getToken(),
                ttlSeconds,
                REFRESH_TTL_SECONDS,
                profile(user)
        );
    }

    private UserProfile profile(User user) {
        return new UserProfile(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported role: " + role);
        }
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
