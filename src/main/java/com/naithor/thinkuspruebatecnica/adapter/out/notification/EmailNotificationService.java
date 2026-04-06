package com.naithor.thinkuspruebatecnica.adapter.out.notification;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.naithor.thinkuspruebatecnica.domain.port.out.NotificationPort;

@Service
@Slf4j
@Primary
public class EmailNotificationService implements NotificationPort {

    @Async
    @Override
    public void send(String contactInfo, String fundName, String message) {
        log.info("[EMAIL] To: {} | Fund: {} | Message: {}", contactInfo, fundName, message);
    }
}
