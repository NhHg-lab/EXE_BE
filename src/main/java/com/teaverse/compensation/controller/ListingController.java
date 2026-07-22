package com.teaverse.compensation.controller;

import com.teaverse.compensation.dto.response.ApiResponse;
import com.teaverse.compensation.dto.request.CreateListingRequest;
import com.teaverse.compensation.dto.response.ListingResponse;
import com.teaverse.compensation.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ApiResponse<List<ListingResponse>> list(@RequestParam(required = false) String game) {
        return ApiResponse.ok(listingService.list(game));
    }

    @PostMapping
    public ApiResponse<ListingResponse> create(@Valid @RequestBody CreateListingRequest request) {
        return ApiResponse.ok("Listing created", listingService.create(request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ListingResponse>> mine() {
        return ApiResponse.ok(listingService.mine());
    }
}
