package com.zentraffic.notification.repository;

import com.zentraffic.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findTop50ByOrderByCreatedAtDesc();
}
