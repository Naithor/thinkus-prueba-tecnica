package com.naithor.thinkuspruebatecnica.adapter.out.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.naithor.thinkuspruebatecnica.domain.port.out.NotificationPort;

@Service
@Slf4j
public class SmsNotificationService implements NotificationPort {

    @Async
    @Override
    public void send(String contactInfo, String fundName, String message) {
        log.info("[SMS] To: {} | Fund: {} | Message: {}", contactInfo, fundName, message);
    }
}
