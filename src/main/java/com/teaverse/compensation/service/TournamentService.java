package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CreateTeamRequest;
import com.teaverse.compensation.dto.request.CreateTournamentRequest;
import com.teaverse.compensation.dto.request.JoinTournamentRequest;
import com.teaverse.compensation.dto.response.RegistrationResponse;
import com.teaverse.compensation.dto.response.TeamResponse;
import com.teaverse.compensation.dto.response.TournamentResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.exception.NotFoundException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.NotificationType;
import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.PaymentPurpose;
import com.teaverse.compensation.model.RegistrationStatus;
import com.teaverse.compensation.model.Team;
import com.teaverse.compensation.model.Tournament;
import com.teaverse.compensation.model.TournamentRegistration;
import com.teaverse.compensation.model.TournamentStatus;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.repository.TeamRepository;
import com.teaverse.compensation.repository.TournamentRegistrationRepository;
import com.teaverse.compensation.repository.TournamentRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final CurrentUserService currentUserService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final DtoMapper mapper;

    public TournamentService(
            TournamentRepository tournamentRepository,
            TeamRepository teamRepository,
            TournamentRegistrationRepository registrationRepository,
            CurrentUserService currentUserService,
            PaymentService paymentService,
            NotificationService notificationService,
            DtoMapper mapper
    ) {
        this.tournamentRepository = tournamentRepository;
        this.teamRepository = teamRepository;
        this.registrationRepository = registrationRepository;
        this.currentUserService = currentUserService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.mapper = mapper;
    }

    public List<TournamentResponse> list(TournamentStatus status, String game) {
        List<Tournament> tournaments;
        if (status != null) {
            tournaments = tournamentRepository.findByStatus(status);
        } else if (game != null && !game.isBlank()) {
            tournaments = tournamentRepository.findByGameIgnoreCase(game);
        } else {
            tournaments = tournamentRepository.findAll();
        }
        return tournaments.stream().map(mapper::toTournamentResponse).toList();
    }

    public TournamentResponse get(String id) {
        return mapper.toTournamentResponse(findTournament(id));
    }

    public TournamentResponse create(CreateTournamentRequest request) {
        User user = currentUserService.getCurrentUser();
        Tournament tournament = new Tournament();
        tournament.setTitle(request.title());
        tournament.setGame(request.game());
        tournament.setStatus(request.status() == null ? TournamentStatus.OPEN : request.status());
        tournament.setMode(request.mode());
        tournament.setFormat(request.format());
        tournament.setTeamSize(request.teamSize() <= 0 ? 5 : request.teamSize());
        tournament.setMaxTeams(request.maxTeams() <= 0 ? 32 : request.maxTeams());
        tournament.setEntryFee(request.entryFee() == null ? BigDecimal.ZERO : request.entryFee());
        tournament.setPrizePool(request.prizePool() == null ? BigDecimal.ZERO : request.prizePool());
        tournament.setStartsAt(request.startsAt());
        tournament.setDescription(request.description());
        tournament.setOrganizerId(user.getId());
        return mapper.toTournamentResponse(tournamentRepository.save(tournament));
    }

    public TeamResponse createTeam(String tournamentId, CreateTeamRequest request) {
        User user = currentUserService.getCurrentUser();
        Tournament tournament = findTournament(tournamentId);
        validateOpenTournament(tournament);

        Team team = new Team();
        team.setTournamentId(tournamentId);
        team.setName(request.name());
        team.setCaptainId(user.getId());
        List<String> members = new ArrayList<>();
        members.add(user.getId());
        if (request.memberIds() != null) {
            request.memberIds().stream().filter(id -> !members.contains(id)).forEach(members::add);
        }
        if (members.size() > tournament.getTeamSize()) {
            throw new BadRequestException("Team member count exceeds tournament team size");
        }
        team.setMemberIds(members);
        return mapper.toTeamResponse(teamRepository.save(team));
    }

    public RegistrationResponse join(String tournamentId, JoinTournamentRequest request, HttpServletRequest servletRequest) {
        User user = currentUserService.getCurrentUser();
        Tournament tournament = findTournament(tournamentId);
        validateOpenTournament(tournament);

        registrationRepository.findByUserIdAndTournamentId(user.getId(), tournamentId)
                .ifPresent(existing -> {
                    throw new BadRequestException("You already registered for this tournament");
                });

        Team team = resolveTeam(tournament, user, request);
        TournamentRegistration registration = new TournamentRegistration();
        registration.setTournamentId(tournamentId);
        registration.setUserId(user.getId());
        registration.setTeamId(team.getId());
        registration.setSmurfScore(calculateSmurfScore(user));
        registration.setSmurfRiskLevel(resolveRisk(registration.getSmurfScore()));

        if ("HIGH".equals(registration.getSmurfRiskLevel())) {
            registration.setStatus(RegistrationStatus.REJECTED);
            registrationRepository.save(registration);
            throw new BadRequestException("Registration rejected by smurf detection risk gate");
        }

        if (tournament.getRegisteredTeams() >= tournament.getMaxTeams()) {
            registration.setStatus(RegistrationStatus.WAITLISTED);
            registration = registrationRepository.save(registration);
            return mapper.toRegistrationResponse(registration, null);
        }

        if (tournament.getEntryFee().compareTo(BigDecimal.ZERO) > 0) {
            registration.setStatus(RegistrationStatus.PENDING_PAYMENT);
            registration = registrationRepository.save(registration);
            Payment payment = paymentService.createPayment(
                    user,
                    PaymentPurpose.TOURNAMENT_ENTRY,
                    registration.getId(),
                    tournament.getEntryFee(),
                    "Tournament entry fee - " + tournament.getTitle(),
                    servletRequest.getRemoteAddr()
            );
            registration.setPaymentId(payment.getId());
            registrationRepository.save(registration);
            return mapper.toRegistrationResponse(registration, payment.getPaymentUrl());
        }

        registration.setStatus(RegistrationStatus.REGISTERED);
        registration = registrationRepository.save(registration);
        tournament.setRegisteredTeams(tournament.getRegisteredTeams() + 1);
        tournamentRepository.save(tournament);
        notificationService.create(user.getId(), NotificationType.TOURNAMENT, "Tournament registration active", "You joined " + tournament.getTitle() + ".");
        return mapper.toRegistrationResponse(registration, null);
    }

    public List<TeamResponse> teams(String tournamentId) {
        findTournament(tournamentId);
        return teamRepository.findByTournamentId(tournamentId).stream().map(mapper::toTeamResponse).toList();
    }

    public List<RegistrationResponse> myRegistrations() {
        User user = currentUserService.getCurrentUser();
        return registrationRepository.findByUserId(user.getId())
                .stream()
                .map(reg -> mapper.toRegistrationResponse(reg, null))
                .toList();
    }

    private Team resolveTeam(Tournament tournament, User user, JoinTournamentRequest request) {
        if (request != null && request.teamId() != null && !request.teamId().isBlank()) {
            Team existing = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new NotFoundException("Team not found"));
            if (!existing.getTournamentId().equals(tournament.getId())) {
                throw new BadRequestException("Team does not belong to this tournament");
            }
            if (!existing.getMemberIds().contains(user.getId())) {
                existing.getMemberIds().add(user.getId());
                if (existing.getMemberIds().size() > tournament.getTeamSize()) {
                    throw new BadRequestException("Team is already full");
                }
                existing = teamRepository.save(existing);
            }
            return existing;
        }

        Team team = new Team();
        team.setTournamentId(tournament.getId());
        team.setName(request != null && request.teamName() != null && !request.teamName().isBlank()
                ? request.teamName()
                : user.getUsername() + " Squad");
        team.setCaptainId(user.getId());
        List<String> members = new ArrayList<>();
        members.add(user.getId());
        if (request != null && request.memberIds() != null) {
            request.memberIds().stream().filter(id -> !members.contains(id)).forEach(members::add);
        }
        if (members.size() > tournament.getTeamSize()) {
            throw new BadRequestException("Team member count exceeds tournament team size");
        }
        team.setMemberIds(members);
        return teamRepository.save(team);
    }

    private Tournament findTournament(String tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament not found"));
    }

    private void validateOpenTournament(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.OPEN && tournament.getStatus() != TournamentStatus.UPCOMING) {
            throw new BadRequestException("Tournament registration is closed");
        }
    }

    private double calculateSmurfScore(User user) {
        double score = 20.0;
        if (user.getTrustScore() < 75) {
            score += 35;
        }
        if (user.getGameProfile().getRank() != null && user.getGameProfile().getRank().equalsIgnoreCase("Challenger")) {
            score += 15;
        }
        if (!user.isVerified()) {
            score += 10;
        }
        return Math.min(100, score);
    }

    private String resolveRisk(double score) {
        if (score >= 75) {
            return "HIGH";
        }
        if (score >= 45) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
