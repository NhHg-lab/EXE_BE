package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.SignInRequest;
import com.teaverse.compensation.dto.request.SignUpRequest;
import com.teaverse.compensation.dto.response.AuthResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.GameProfile;
import com.teaverse.compensation.model.NotificationType;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.model.UserRole;
import com.teaverse.compensation.repository.UserRepository;
import com.teaverse.compensation.security.JwtService;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final DtoMapper mapper;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            DtoMapper mapper,
            NotificationService notificationService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public AuthResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() == null ? UserRole.GAMER : request.role());
        user.setGameProfile(request.gameProfile() == null ? defaultProfile() : request.gameProfile());
        user.setTrustScore(calculateInitialTrust(user));
        user = userRepository.save(user);

        notificationService.create(
                user.getId(),
                NotificationType.SYSTEM,
                "Welcome to GameTrust",
                "Your GameTrust account has been created. Build your profile and join a tournament."
        );
        auditService.record(user.getId(), "USER_SIGN_UP", "USER", user.getId(), Map.of("role", user.getRole().name()));

        return buildAuthResponse(user);
    }

    public AuthResponse signIn(SignInRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails details = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                java.util.List.of()
        );
        String token = jwtService.generateToken(details);
        return new AuthResponse(token, mapper.toUserResponse(user));
    }

    private GameProfile defaultProfile() {
        GameProfile profile = new GameProfile();
        profile.setMainGame("Arena of Valor");
        profile.setRank("Platinum");
        profile.setGoal("Compete");
        profile.setPreferredRole("Flex");
        profile.setOnlineTime("Evening");
        return profile;
    }

    private double calculateInitialTrust(User user) {
        double score = 82.0;
        if (user.getRole() == UserRole.SELLER) {
            score -= 3.0;
        }
        if (user.getGameProfile().getMainGame() != null) {
            score += 3.0;
        }
        return Math.min(99.0, score);
    }
}
