package com.teaverse.compensation.scheduler;

import com.teaverse.compensation.model.BracketMatch;
import com.teaverse.compensation.model.Clan;
import com.teaverse.compensation.model.ClanStatus;
import com.teaverse.compensation.model.GameProfile;
import com.teaverse.compensation.model.Listing;
import com.teaverse.compensation.model.Post;
import com.teaverse.compensation.model.PostType;
import com.teaverse.compensation.model.Tournament;
import com.teaverse.compensation.model.TournamentStatus;
import com.teaverse.compensation.model.User;
import com.teaverse.compensation.model.UserRole;
import com.teaverse.compensation.repository.ClanRepository;
import com.teaverse.compensation.repository.ListingRepository;
import com.teaverse.compensation.repository.PostRepository;
import com.teaverse.compensation.repository.TournamentRepository;
import com.teaverse.compensation.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedDataRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final ClanRepository clanRepository;
    private final PostRepository postRepository;
    private final ListingRepository listingRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataRunner(
            UserRepository userRepository,
            TournamentRepository tournamentRepository,
            ClanRepository clanRepository,
            PostRepository postRepository,
            ListingRepository listingRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.clanRepository = clanRepository;
        this.postRepository = postRepository;
        this.listingRepository = listingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        try {
            if (userRepository.count() < 3) {
                userRepository.deleteAll();
                userRepository.saveAll(seedUsers());
            }
            if (tournamentRepository.count() < 3) {
                tournamentRepository.deleteAll();
                tournamentRepository.saveAll(seedTournaments());
            }
            if (clanRepository.count() < 3) {
                clanRepository.deleteAll();
                clanRepository.saveAll(seedClans());
            }
            if (postRepository.count() < 3) {
                postRepository.deleteAll();
                postRepository.saveAll(seedPosts());
            }
            if (listingRepository.count() < 5) {
                listingRepository.deleteAll();
                listingRepository.saveAll(seedListings());
            }
            log.info("GameTrust seed data checked and populated successfully");
        } catch (Exception ex) {
            log.warn("Skipping seed data: {}", ex.getMessage());
        }
    }

    private List<User> seedUsers() {
        User gamer = user("gamer@gametrust.dev", "player_one", "Player One", UserRole.GAMER, "Arena of Valor", "Diamond", "Compete", 92);
        User creator = user("creator@gametrust.dev", "neon_creator", "Neon Creator", UserRole.CREATOR, "FreeFire", "Master", "Create content", 95);
        User seller = user("seller@gametrust.dev", "axiom_seller", "Axiom Seller", UserRole.SELLER, "Arena of Valor", "Platinum", "Sell verified accounts", 90);
        return List.of(gamer, creator, seller);
    }

    private User user(String email, String username, String fullName, UserRole role, String game, String rank, String goal, double trust) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setTrustScore(trust);
        user.setVerified(true);
        GameProfile profile = new GameProfile();
        profile.setMainGame(game);
        profile.setRank(rank);
        profile.setGoal(goal);
        profile.setPreferredRole("Flex");
        profile.setOnlineTime("Evening");
        profile.setFavoriteGames(List.of(game));
        user.setGameProfile(profile);
        return user;
    }

    private List<Tournament> seedTournaments() {
        Tournament neon = tournament("NEON CIRCUIT OPEN", "FreeFire", TournamentStatus.OPEN, "4v4 Double Elim", "4v4", 4, 128, 15000, 50000000);
        neon.setBracket(List.of(
                match("Quarter", 1, "PHANTOM SYNDICATE", "NEON WOLVES", "", "", false),
                match("Quarter", 2, "DARK VECTOR", "GRID REAPERS", "", "", false),
                match("Semi", 1, "TBD", "TBD", "", "", false),
                match("Final", 1, "TBD", "TBD", "", "", false)
        ));

        Tournament dark = tournament("DARKBYTE INVITATIONAL", "Arena of Valor", TournamentStatus.LIVE, "5v5 Single Elim", "5v5", 5, 64, 10000, 25000000);
        dark.setBracket(List.of(
                match("Quarter", 1, "TEAM SOLOMON", "VORTEX ESPORTS", "2", "1", false),
                match("Quarter", 2, "ALPHA SQUAD", "BETA NINJAS", "LIVE", "", true)
        ));

        Tournament phantom = tournament("PHANTOM LEAGUE S3", "FreeFire", TournamentStatus.UPCOMING, "Squad Battle", "Squads", 4, 256, 20000, 100000000);
        return List.of(neon, dark, phantom);
    }

    private Tournament tournament(
            String title,
            String game,
            TournamentStatus status,
            String mode,
            String format,
            int teamSize,
            int maxTeams,
            int entryFee,
            int prizePool
    ) {
        Tournament tournament = new Tournament();
        tournament.setTitle(title);
        tournament.setGame(game);
        tournament.setStatus(status);
        tournament.setMode(mode);
        tournament.setFormat(format);
        tournament.setTeamSize(teamSize);
        tournament.setMaxTeams(maxTeams);
        tournament.setEntryFee(BigDecimal.valueOf(entryFee));
        tournament.setPrizePool(BigDecimal.valueOf(prizePool));
        tournament.setStartsAt(Instant.now().plus(2, ChronoUnit.DAYS));
        tournament.setDescription("Community tournament with GameTrust registration, bracket tracking, and smurf risk checks.");
        return tournament;
    }

    private BracketMatch match(String round, int seed, String teamA, String teamB, String scoreA, String scoreB, boolean live) {
        BracketMatch match = new BracketMatch();
        match.setRound(round);
        match.setSeed(seed);
        match.setTeamA(teamA);
        match.setTeamB(teamB);
        match.setScoreA(scoreA);
        match.setScoreB(scoreB);
        match.setLive(live);
        return match;
    }

    private List<Clan> seedClans() {
        return List.of(
                clan("PHANTOM SYNDICATE", "PSY", "ELITE", "Global", ClanStatus.INVITE_ONLY, 9842, 412),
                clan("GRID REAPERS", "GR", "ALPHA", "SEA", ClanStatus.OPEN, 9402, 298),
                clan("CYBER UNIT 7", "CU7", "BETA", "SEA", ClanStatus.OPEN, 9261, 254)
        );
    }

    private Clan clan(String name, String tag, String tier, String region, ClanStatus status, int rating, int wins) {
        Clan clan = new Clan();
        clan.setName(name);
        clan.setTag(tag);
        clan.setTier(tier);
        clan.setRegion(region);
        clan.setStatus(status);
        clan.setRating(rating);
        clan.setWins(wins);
        clan.setGames(List.of("FreeFire", "Arena of Valor"));
        clan.setRequirement(tier.equals("ELITE") ? "Trust Score 9.5+" : "Trust Score 7.5+");
        clan.setDescription("Competitive guild for tournament squads, creators, and trusted marketplace members.");
        return clan;
    }

    private List<Post> seedPosts() {
        Post p1 = post("neon_creator", PostType.TOURNAMENT, "NEON CIRCUIT OPEN registration is live. Build your squad and lock a slot through GameTrust.", "FreeFire", "PSY", 512);
        Post p2 = post("axiom_seller", PostType.LISTING, "Verified Arena of Valor account listing is up. Buyer risk analysis and VNPay checkout supported.", "Arena of Valor", "", 188);
        Post p3 = post("player_one", PostType.RECRUIT, "Looking for evening ranked teammates for AOV. Diamond plus, tournament mindset.", "Arena of Valor", "GR", 67);
        return List.of(p1, p2, p3);
    }

    private Post post(String author, PostType type, String content, String game, String clanTag, int likes) {
        Post post = new Post();
        post.setAuthorName(author);
        post.setType(type);
        post.setContent(content);
        post.setGame(game);
        post.setClanTag(clanTag);
        post.setLikes(likes);
        return post;
    }

    private List<Listing> seedListings() {
        return List.of(
                listing("Arena of Valor Conqueror Account - Full Champions + SGP Skin", "Arena of Valor", "AS", "Conqueror", 189500, 9.6),
                listing("FreeFire Heroic Account - All EVO Guns Maxed", "FreeFire", "SEA", "Heroic", 249000, 9.8),
                listing("Valorant Radiant Account - VCT Vandal & Karambit", "Valorant", "SEA", "Radiant", 1250000, 9.9),
                listing("League of Legends Challenger Account - 150+ Skins", "League of Legends", "VN", "Challenger", 850000, 9.5),
                listing("PUBG Mobile Grandmaster Account - Glacier M416 Max", "PUBG Mobile", "SEA", "Grandmaster", 2100000, 9.7),
                listing("Genshin Impact AR60 Account - C6 Raiden & Signature Weapon", "Genshin Impact", "Asia", "AR60", 1800000, 9.6)
        );
    }

    private Listing listing(String title, String game, String server, String rank, int price, double trust) {
        Listing listing = new Listing();
        listing.setTitle(title);
        listing.setGame(game);
        listing.setServer(server);
        listing.setRankBadge(rank);
        listing.setPrice(BigDecimal.valueOf(price));
        listing.setTrustScore(trust);
        listing.setDescription("Verified seller listing with trust score, anti-smurf audit, and safe transaction workflow.");
        return listing;
    }
}
