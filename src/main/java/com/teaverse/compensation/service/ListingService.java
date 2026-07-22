package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.CreateListingRequest;
import com.teaverse.compensation.dto.response.ListingResponse;
import com.teaverse.compensation.exception.BadRequestException;
import com.teaverse.compensation.mapper.DtoMapper;
import com.teaverse.compensation.model.Listing;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.model.UserRole;
import com.teaverse.compensation.repository.ListingRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ListingService {
    private final ListingRepository listingRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public ListingService(ListingRepository listingRepository, CurrentUserService currentUserService, DtoMapper mapper) {
        this.listingRepository = listingRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    public List<ListingResponse> list(String game) {
        List<Listing> listings = game == null || game.isBlank() || "ALL".equalsIgnoreCase(game)
                ? listingRepository.findByActiveTrue()
                : listingRepository.findByGameIgnoreCaseAndActiveTrue(game);
        return listings.stream()
                .sorted(Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(mapper::toListingResponse)
                .toList();
    }

    public ListingResponse create(CreateListingRequest request) {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.SELLER && user.getRole() != UserRole.CREATOR) {
            throw new BadRequestException("Only seller or creator accounts can create listings");
        }

        Listing listing = new Listing();
        listing.setSellerId(user.getId());
        listing.setTitle(request.title());
        listing.setGame(request.game());
        listing.setServer(request.server());
        listing.setRankBadge(request.rankBadge());
        listing.setDescription(request.description());
        listing.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        listing.setTrustScore(user.getTrustScore());
        return mapper.toListingResponse(listingRepository.save(listing));
    }

    public List<ListingResponse> mine() {
        User user = currentUserService.getCurrentUser();
        return listingRepository.findBySellerId(user.getId()).stream().map(mapper::toListingResponse).toList();
    }
}
