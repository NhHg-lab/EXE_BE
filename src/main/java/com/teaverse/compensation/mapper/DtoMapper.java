package com.teaverse.compensation.mapper;

import com.teaverse.compensation.dto.response.ClanResponse;
import com.teaverse.compensation.dto.response.ListingResponse;
import com.teaverse.compensation.dto.response.NotificationResponse;
import com.teaverse.compensation.dto.response.PaymentResponse;
import com.teaverse.compensation.dto.response.PostResponse;
import com.teaverse.compensation.dto.response.RegistrationResponse;
import com.teaverse.compensation.dto.response.TeamResponse;
import com.teaverse.compensation.dto.response.TournamentResponse;
import com.teaverse.compensation.dto.response.UserResponse;
import com.teaverse.compensation.model.Clan;
import com.teaverse.compensation.model.Listing;
import com.teaverse.compensation.model.Notification;
import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.Post;
import com.teaverse.compensation.model.Team;
import com.teaverse.compensation.model.Tournament;
import com.teaverse.compensation.model.TournamentRegistration;
import com.teaverse.compensation.model.User;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isPremium(),
                user.isVerified(),
                user.getTrustScore(),
                user.getGameProfile(),
                user.getCreatedAt()
        );
    }

    public TournamentResponse toTournamentResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getTitle(),
                tournament.getGame(),
                tournament.getStatus(),
                tournament.getMode(),
                tournament.getFormat(),
                tournament.getTeamSize(),
                tournament.getMaxTeams(),
                tournament.getRegisteredTeams(),
                tournament.getEntryFee(),
                tournament.getPrizePool(),
                tournament.getStartsAt(),
                tournament.getOrganizerId(),
                tournament.getDescription(),
                tournament.getBracket()
        );
    }

    public TeamResponse toTeamResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getTournamentId(),
                team.getName(),
                team.getCaptainId(),
                team.getMemberIds()
        );
    }

    public RegistrationResponse toRegistrationResponse(TournamentRegistration registration, String paymentUrl) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getTournamentId(),
                registration.getUserId(),
                registration.getTeamId(),
                registration.getStatus(),
                registration.getSmurfScore(),
                registration.getSmurfRiskLevel(),
                registration.getPaymentId(),
                paymentUrl
        );
    }

    public ClanResponse toClanResponse(Clan clan, String currentUserId) {
        boolean joined = currentUserId != null && clan.getMemberIds().contains(currentUserId);
        return new ClanResponse(
                clan.getId(),
                clan.getName(),
                clan.getTag(),
                clan.getTier(),
                clan.getRegion(),
                clan.getDescription(),
                clan.getGames(),
                clan.getRequirement(),
                clan.getStatus(),
                clan.getWins(),
                clan.getRating(),
                clan.getMemberIds().size(),
                joined
        );
    }

    public PostResponse toPostResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getType(),
                post.getContent(),
                post.getGame(),
                post.getClanTag(),
                post.getLikes(),
                post.getComments(),
                post.isSponsored(),
                post.getCreatedAt()
        );
    }

    public ListingResponse toListingResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getSellerId(),
                listing.getTitle(),
                listing.getGame(),
                listing.getServer(),
                listing.getRankBadge(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getTrustScore(),
                listing.isActive(),
                listing.getCreatedAt()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPurpose(),
                payment.getReferenceId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getTransactionRef(),
                payment.getPaymentUrl()
        );
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.isUnread(),
                notification.getCreatedAt()
        );
    }
}
