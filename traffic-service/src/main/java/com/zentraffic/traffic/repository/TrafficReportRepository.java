package com.zentraffic.traffic.repository;

import com.zentraffic.traffic.entity.TrafficReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TrafficReportRepository extends JpaRepository<TrafficReport, Long> {
    long countByRoadIdAndReportTypeIgnoreCaseAndCreatedAtAfter(Long roadId, String reportType, Instant after);
    List<TrafficReport> findTop20ByOrderByCreatedAtDesc();
}
