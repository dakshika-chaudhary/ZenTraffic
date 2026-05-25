package com.zentraffic.notification.controller;

import com.zentraffic.common.dto.NotificationRequest;
import com.zentraffic.notification.entity.Notification;
import com.zentraffic.notification.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notify")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public Notification send(@RequestBody NotificationRequest request) {
        return notificationService.send(request);
    }

    @GetMapping
    public List<Notification> latest(@RequestParam(required = false) Long userId) {
        return notificationService.latest(userId);
    }
}
