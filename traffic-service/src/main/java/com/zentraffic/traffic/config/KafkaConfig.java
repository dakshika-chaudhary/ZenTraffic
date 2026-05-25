package com.zentraffic.traffic.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    NewTopic accidentTopic() {
        return new NewTopic("traffic.accident.reported", 1, (short) 1);
    }

    @Bean
    NewTopic congestionTopic() {
        return new NewTopic("traffic.congestion.detected", 1, (short) 1);
    }

    @Bean
    NewTopic roadStatusTopic() {
        return new NewTopic("traffic.road.status", 1, (short) 1);
    }
}
