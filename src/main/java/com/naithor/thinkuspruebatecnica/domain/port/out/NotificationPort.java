package com.naithor.thinkuspruebatecnica.domain.port.out;

public interface NotificationPort {
    void send(String contactInfo, String fundName, String message);
}
