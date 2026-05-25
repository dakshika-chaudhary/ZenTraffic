package com.zentraffic.traffic.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "traffic_reports", indexes = @Index(name = "idx_reports_road_created", columnList = "roadId,createdAt"))
public class TrafficReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long roadId;
    private String reportType;
    private int severity;
    @Column(length = 1000)
    private String description;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoadId() { return roadId; }
    public void setRoadId(Long roadId) { this.roadId = roadId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public int getSeverity() { return severity; }
    public void setSeverity(int severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
}
