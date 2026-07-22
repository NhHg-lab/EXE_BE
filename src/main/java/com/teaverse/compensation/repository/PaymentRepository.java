package com.teaverse.compensation.repository;

import com.teaverse.compensation.model.Payment;
import com.teaverse.compensation.model.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByTransactionRef(String transactionRef);

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatus(PaymentStatus status);
}
