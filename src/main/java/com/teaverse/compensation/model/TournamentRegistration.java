package com.teaverse.compensation.model;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tournament_registrations")
@CompoundIndex(name = "user_tournament_unique", def = "{'userId': 1, 'tournamentId': 1}", unique = true)
public class TournamentRegistration {
    @Id
    private String id;
    private String tournamentId;
    private String userId;
    private String teamId;
    private RegistrationStatus status = RegistrationStatus.PENDING_PAYMENT;
    private double smurfScore;
    private String smurfRiskLevel;
    private String paymentId;

    @CreatedDate
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public double getSmurfScore() {
        return smurfScore;
    }

    public void setSmurfScore(double smurfScore) {
        this.smurfScore = smurfScore;
    }

    public String getSmurfRiskLevel() {
        return smurfRiskLevel;
    }

    public void setSmurfRiskLevel(String smurfRiskLevel) {
        this.smurfRiskLevel = smurfRiskLevel;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
