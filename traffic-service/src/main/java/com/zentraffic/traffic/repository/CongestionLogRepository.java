package com.zentraffic.traffic.repository;

import com.zentraffic.traffic.entity.CongestionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CongestionLogRepository extends JpaRepository<CongestionLog, Long> {
}
