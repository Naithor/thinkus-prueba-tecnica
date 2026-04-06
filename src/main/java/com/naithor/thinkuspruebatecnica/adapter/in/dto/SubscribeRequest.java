package com.naithor.thinkuspruebatecnica.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
        @NotBlank String clientId
) {
}
