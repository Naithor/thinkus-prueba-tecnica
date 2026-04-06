package com.naithor.thinkuspruebatecnica.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.naithor.thinkuspruebatecnica.adapter.in.dto.FundResponse;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.SubscribeRequest;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.TransactionResponse;
import com.naithor.thinkuspruebatecnica.domain.model.Transaction;
import com.naithor.thinkuspruebatecnica.domain.port.in.CancelSubscriptionUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.in.SubscribeFundUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.FundPort;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/funds")
@RequiredArgsConstructor
@Tag(name = "Funds", description = "Investment fund operations")
public class FundController {

    private final SubscribeFundUseCase subscribeFundUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
    private final FundPort fundPort;

    @PostMapping("/{fundId}/subscribe")
    @Operation(summary = "Subscribe to a fund")
    public ResponseEntity<TransactionResponse> subscribe(
            @PathVariable Integer fundId,
            @Valid @RequestBody SubscribeRequest request) {
        Transaction tx = subscribeFundUseCase.execute(request.clientId(), fundId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransactionResponse.from(tx));
    }

    @DeleteMapping("/{fundId}/subscribe")
    @Operation(summary = "Cancel fund subscription")
    public ResponseEntity<TransactionResponse> cancel(
            @PathVariable Integer fundId,
            @RequestParam String clientId) {
        Transaction tx = cancelSubscriptionUseCase.execute(clientId, fundId);
        return ResponseEntity.ok(TransactionResponse.from(tx));
    }

    @GetMapping
    @Operation(summary = "List all available funds")
    public ResponseEntity<List<FundResponse>> listFunds() {
        var funds = fundPort.findAll().stream()
                .map(FundResponse::from)
                .toList();
        return ResponseEntity.ok(funds);
    }
}
