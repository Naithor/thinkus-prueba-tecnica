package com.naithor.thinkuspruebatecnica.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    private String id;

    @Version
    private Long version;

    private BigDecimal balance;

    private NotificationPreference notificationPreference;

    private String contactInfo;

    public static Client newDefaultClient() {
        return Client.builder()
                .id(UUID.randomUUID().toString())
                .balance(new BigDecimal("500000"))
                .notificationPreference(NotificationPreference.EMAIL)
                .contactInfo("")
                .build();
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}
