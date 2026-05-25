package com.zentraffic.notification.service;

import com.zentraffic.common.dto.NotificationRequest;
import com.zentraffic.common.event.AccidentReportedEvent;
import com.zentraffic.common.event.CongestionEvent;
import com.zentraffic.notification.entity.Notification;
import com.zentraffic.notification.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public Notification send(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setMessage(request.message());
        notification.setPriority(request.priority() == null ? "NORMAL" : request.priority().toUpperCase());
        return notifications.save(notification);
    }

    public List<Notification> latest(Long userId) {
        return userId == null ? notifications.findTop50ByOrderByCreatedAtDesc() : notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @KafkaListener(topics = "traffic.accident.reported", groupId = "notification-service")
    public void accident(AccidentReportedEvent event) {
        send(new NotificationRequest(null,
                "Accident reported on " + event.roadName() + ". Use an alternate route.",
                "HIGH"));
    }

    @KafkaListener(topics = "traffic.congestion.detected", groupId = "notification-service")
    public void congestion(CongestionEvent event) {
        send(new NotificationRequest(null,
                "Heavy traffic detected on " + event.roadName() + ". Alternative route recommended.",
                "HIGH"));
    }
}
