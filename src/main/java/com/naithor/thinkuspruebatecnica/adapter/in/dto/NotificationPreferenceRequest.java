package com.naithor.thinkuspruebatecnica.adapter.in.dto;

import com.naithor.thinkuspruebatecnica.domain.model.NotificationPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
        @NotNull NotificationPreference preference,
        @NotBlank String contactInfo
) {
}
