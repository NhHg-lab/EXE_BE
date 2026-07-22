package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Team;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByTournamentId(String tournamentId);
}
