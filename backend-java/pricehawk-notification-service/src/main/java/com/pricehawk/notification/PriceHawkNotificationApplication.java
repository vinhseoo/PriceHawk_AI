package com.pricehawk.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceHawkNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriceHawkNotificationApplication.class, args);
    }
}
