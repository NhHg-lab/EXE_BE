package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.CreateClanRequest;
import com.teaverse.compensation.dto.response.ClanResponse;
import com.teaverse.compensation.service.ClanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clans")
public class ClanController {
    private final ClanService clanService;

    public ClanController(ClanService clanService) {
        this.clanService = clanService;
    }

    @GetMapping
    public ApiResponse<List<ClanResponse>> list(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.ok(clanService.list(tier, region));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClanResponse> get(@PathVariable String id) {
        return ApiResponse.ok(clanService.get(id));
    }

    @PostMapping
    public ApiResponse<ClanResponse> create(@Valid @RequestBody CreateClanRequest request) {
        return ApiResponse.ok("Clan created", clanService.create(request));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<ClanResponse> join(@PathVariable String id) {
        return ApiResponse.ok("Clan joined", clanService.join(id));
    }

    @DeleteMapping("/{id}/leave")
    public ApiResponse<ClanResponse> leave(@PathVariable String id) {
        return ApiResponse.ok("Clan left", clanService.leave(id));
    }
}
