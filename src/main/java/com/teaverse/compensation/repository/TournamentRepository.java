package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Tournament;
import com.teaverse.compensation.model.TournamentStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRepository extends MongoRepository<Tournament, String> {
    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByGameIgnoreCase(String game);
}
