package com.zentraffic.traffic.repository;

import com.zentraffic.traffic.entity.Road;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadRepository extends JpaRepository<Road, Long> {
    List<Road> findByStatusIn(List<String> statuses);
}
