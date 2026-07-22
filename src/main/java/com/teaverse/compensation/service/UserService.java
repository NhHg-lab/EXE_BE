package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.UpdateProfileRequest;
import com.teaverse.compensation.dto.response.MatchResponse;
import com.teaverse.compensation.dto.response.UserResponse;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.GameProfile;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.model.UserRole;
import com.teaverse.compensation.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public UserService(UserRepository userRepository, CurrentUserService currentUserService, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    public UserResponse me() {
        return mapper.toUserResponse(currentUserService.getCurrentUser());
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setFullName(request.fullName());
        user.setGameProfile(request.gameProfile());
        user.setTrustScore(recalculateTrust(user));
        return mapper.toUserResponse(userRepository.save(user));
    }

    public List<MatchResponse> smartMatches() {
        User current = currentUserService.getCurrentUser();
        return userRepository.findByRole(UserRole.GAMER)
                .stream()
                .filter(candidate -> !candidate.getId().equals(current.getId()))
                .map(candidate -> toMatch(current, candidate))
                .sorted(Comparator.comparingInt(MatchResponse::matchScore).reversed())
                .limit(12)
                .toList();
    }

    private MatchResponse toMatch(User current, User candidate) {
        GameProfile a = current.getGameProfile();
        GameProfile b = candidate.getGameProfile();
        int score = 30;
        if (equalsIgnoreCase(a.getMainGame(), b.getMainGame())) {
            score += 30;
        }
        if (equalsIgnoreCase(a.getRank(), b.getRank())) {
            score += 20;
        }
        if (equalsIgnoreCase(a.getGoal(), b.getGoal())) {
            score += 15;
        }
        if (equalsIgnoreCase(a.getOnlineTime(), b.getOnlineTime())) {
            score += 10;
        }
        if (candidate.isPremium()) {
            score += 5;
        }
        return new MatchResponse(
                candidate.getId(),
                candidate.getUsername(),
                b.getMainGame(),
                b.getRank(),
                b.getGoal(),
                candidate.getTrustScore(),
                Math.min(100, score)
        );
    }

    private double recalculateTrust(User user) {
        double score = 80.0;
        GameProfile profile = user.getGameProfile();
        if (profile.getMainGame() != null && !profile.getMainGame().isBlank()) {
            score += 5.0;
        }
        if (profile.getRank() != null && !profile.getRank().isBlank()) {
            score += 5.0;
        }
        if (profile.getGoal() != null && !profile.getGoal().isBlank()) {
            score += 4.0;
        }
        if (user.isVerified()) {
            score += 5.0;
        }
        return Math.min(99.0, score);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
