package com.zentraffic.websocket.service;

import com.zentraffic.common.event.AccidentReportedEvent;
import com.zentraffic.common.event.CongestionEvent;
import com.zentraffic.common.event.RoadStatusEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrafficBroadcastService {
    private final SimpMessagingTemplate messagingTemplate;

    public TrafficBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "traffic.road.status", groupId = "websocket-service")
    public void roadStatus(RoadStatusEvent event) {
        messagingTemplate.convertAndSend("/topic/traffic", event);
        messagingTemplate.convertAndSend("/topic/roads/" + event.roadId(), event);
    }

    @KafkaListener(topics = "traffic.accident.reported", groupId = "websocket-service")
    public void accident(AccidentReportedEvent event) {
        messagingTemplate.convertAndSend("/topic/alerts", event);
    }

    @KafkaListener(topics = "traffic.congestion.detected", groupId = "websocket-service")
    public void congestion(CongestionEvent event) {
        messagingTemplate.convertAndSend("/topic/congestion", event);
    }
}
