package com.zentraffic.traffic.service;

import org.springframework.stereotype.Component;

@Component
public class TrafficRules {
    public int congestionScore(int vehicleCount, double avgSpeed, int severity) {
        int densityPart = Math.min(70, Math.max(0, vehicleCount));
        int speedPenalty = avgSpeed < 15 ? 25 : avgSpeed < 30 ? 15 : avgSpeed < 45 ? 8 : 0;
        int severityPart = Math.min(15, Math.max(0, severity * 3));
        return Math.min(100, densityPart + speedPenalty + severityPart);
    }

    public String statusFor(int score, String reportType) {
        if ("ROADBLOCK".equalsIgnoreCase(reportType)) {
            return "BLOCKED";
        }
        if ("ACCIDENT".equalsIgnoreCase(reportType)) {
            return "ACCIDENT";
        }
        if (score >= 75) {
            return "HEAVY";
        }
        if (score >= 45) {
            return "MODERATE";
        }
        return "CLEAR";
    }
}
