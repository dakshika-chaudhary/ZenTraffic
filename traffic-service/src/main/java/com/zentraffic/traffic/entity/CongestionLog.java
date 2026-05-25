package com.zentraffic.traffic.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "congestion_logs", indexes = @Index(name = "idx_congestion_road_timestamp", columnList = "roadId,timestamp"))
public class CongestionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long roadId;
    private int densityScore;
    private Instant timestamp = Instant.now();

    public Long getId() { return id; }
    public Long getRoadId() { return roadId; }
    public void setRoadId(Long roadId) { this.roadId = roadId; }
    public int getDensityScore() { return densityScore; }
    public void setDensityScore(int densityScore) { this.densityScore = densityScore; }
    public Instant getTimestamp() { return timestamp; }
}
