package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CreateClanRequest;
import com.teaverse.compensation.dto.response.ClanResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.exception.NotFoundException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.Clan;
import com.teaverse.compensation.model.ClanStatus;
import com.teaverse.compensation.model.NotificationType;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.repository.ClanRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClanService {
    private final ClanRepository clanRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final DtoMapper mapper;

    public ClanService(
            ClanRepository clanRepository,
            CurrentUserService currentUserService,
            NotificationService notificationService,
            DtoMapper mapper
    ) {
        this.clanRepository = clanRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    public List<ClanResponse> list(String tier, String region) {
        String currentUserId = currentUserService.getCurrentUserIdOrNull();
        return clanRepository.findAll()
                .stream()
                .filter(clan -> tier == null || tier.isBlank() || "ALL".equalsIgnoreCase(tier) || clan.getTier().equalsIgnoreCase(tier))
                .filter(clan -> region == null || region.isBlank() || "ALL".equalsIgnoreCase(region) || clan.getRegion().equalsIgnoreCase(region))
                .sorted(Comparator.comparingInt(Clan::getRating).reversed())
                .map(clan -> mapper.toClanResponse(clan, currentUserId))
                .toList();
    }

    public ClanResponse get(String id) {
        return mapper.toClanResponse(findClan(id), currentUserService.getCurrentUserIdOrNull());
    }

    public ClanResponse create(CreateClanRequest request) {
        User user = currentUserService.getCurrentUser();
        clanRepository.findByTag(request.tag()).ifPresent(clan -> {
            throw new BadRequestException("Clan tag already exists");
        });

        Clan clan = new Clan();
        clan.setName(request.name());
        clan.setTag(request.tag().toUpperCase());
        clan.setTier(request.tier() == null ? "BETA" : request.tier());
        clan.setRegion(request.region() == null ? "SEA" : request.region());
        clan.setDescription(request.description());
        clan.setGames(request.games());
        clan.setRequirement(request.requirement() == null ? "Trust Score 7.5+" : request.requirement());
        clan.setStatus(request.status() == null ? ClanStatus.OPEN : request.status());
        clan.setRating(8500);
        clan.getMemberIds().add(user.getId());
        return mapper.toClanResponse(clanRepository.save(clan), user.getId());
    }

    public ClanResponse join(String id) {
        User user = currentUserService.getCurrentUser();
        Clan clan = findClan(id);
        if (clan.getStatus() == ClanStatus.INVITE_ONLY) {
            throw new BadRequestException("Clan is invite only");
        }
        if (clan.getRequirement() != null && clan.getRequirement().contains("9.5") && user.getTrustScore() < 95) {
            throw new BadRequestException("Your Trust Score is below this clan requirement");
        }
        if (!clan.getMemberIds().contains(user.getId())) {
            clan.getMemberIds().add(user.getId());
        }
        clan = clanRepository.save(clan);
        notificationService.create(user.getId(), NotificationType.CLAN, "Clan joined", "You joined [" + clan.getTag() + "] " + clan.getName() + ".");
        return mapper.toClanResponse(clan, user.getId());
    }

    public ClanResponse leave(String id) {
        User user = currentUserService.getCurrentUser();
        Clan clan = findClan(id);
        clan.getMemberIds().remove(user.getId());
        clan = clanRepository.save(clan);
        notificationService.create(user.getId(), NotificationType.CLAN, "Clan left", "You left [" + clan.getTag() + "] " + clan.getName() + ".");
        return mapper.toClanResponse(clan, user.getId());
    }

    private Clan findClan(String id) {
        return clanRepository.findById(id).orElseThrow(() -> new NotFoundException("Clan not found"));
    }
}
