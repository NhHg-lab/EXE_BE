package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.TournamentRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRegistrationRepository extends MongoRepository<TournamentRegistration, String> {
    List<TournamentRegistration> findByTournamentId(String tournamentId);

    List<TournamentRegistration> findByUserId(String userId);

    Optional<TournamentRegistration> findByUserIdAndTournamentId(String userId, String tournamentId);
}
