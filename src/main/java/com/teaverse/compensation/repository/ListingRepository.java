package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Listing;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ListingRepository extends MongoRepository<Listing, String> {
    List<Listing> findByActiveTrue();

    List<Listing> findBySellerId(String sellerId);

    List<Listing> findByGameIgnoreCaseAndActiveTrue(String game);
}
