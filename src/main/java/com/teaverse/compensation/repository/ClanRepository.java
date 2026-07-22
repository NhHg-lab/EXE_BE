package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Clan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClanRepository extends MongoRepository<Clan, String> {
    Optional<Clan> findByTag(String tag);

    List<Clan> findByTierIgnoreCase(String tier);

    List<Clan> findByRegionIgnoreCase(String region);
}
