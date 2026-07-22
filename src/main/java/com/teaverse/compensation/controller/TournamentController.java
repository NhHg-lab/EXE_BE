package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.CreateTeamRequest;
import com.teaverse.compensation.dto.request.CreateTournamentRequest;
import com.teaverse.compensation.dto.request.JoinTournamentRequest;
import com.teaverse.compensation.dto.response.RegistrationResponse;
import com.teaverse.compensation.dto.response.TeamResponse;
import com.teaverse.compensation.dto.response.TournamentResponse;
import com.teaverse.compensation.model.TournamentStatus;
import com.teaverse.compensation.service.TournamentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public ApiResponse<List<TournamentResponse>> list(
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) String game
    ) {
        return ApiResponse.ok(tournamentService.list(status, game));
    }

    @GetMapping("/{id}")
    public ApiResponse<TournamentResponse> get(@PathVariable String id) {
        return ApiResponse.ok(tournamentService.get(id));
    }

    @PostMapping
    public ApiResponse<TournamentResponse> create(@Valid @RequestBody CreateTournamentRequest request) {
        return ApiResponse.ok("Tournament created", tournamentService.create(request));
    }

    @PostMapping("/{id}/teams")
    public ApiResponse<TeamResponse> createTeam(@PathVariable String id, @Valid @RequestBody CreateTeamRequest request) {
        return ApiResponse.ok("Team created", tournamentService.createTeam(id, request));
    }

    @GetMapping("/{id}/teams")
    public ApiResponse<List<TeamResponse>> teams(@PathVariable String id) {
        return ApiResponse.ok(tournamentService.teams(id));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<RegistrationResponse> join(
            @PathVariable String id,
            @RequestBody(required = false) JoinTournamentRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.ok("Tournament registration started", tournamentService.join(id, request, servletRequest));
    }

    @GetMapping("/registrations/me")
    public ApiResponse<List<RegistrationResponse>> myRegistrations() {
        return ApiResponse.ok(tournamentService.myRegistrations());
    }
}
