package com.naithor.thinkuspruebatecnica.adapter.in.web;

import com.naithor.thinkuspruebatecnica.adapter.in.dto.NotificationPreferenceRequest;
import com.naithor.thinkuspruebatecnica.adapter.in.dto.TransactionResponse;
import com.naithor.thinkuspruebatecnica.domain.port.in.GetTransactionHistoryUseCase;
import com.naithor.thinkuspruebatecnica.domain.port.out.ClientPort;
import com.naithor.thinkuspruebatecnica.exception.ClientNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Client operations")
public class ClientController {

    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final ClientPort clientPort;

    @GetMapping("/{clientId}/transactions")
    @Operation(summary = "Get transaction history")
    public ResponseEntity<List<TransactionResponse>> getHistory(@PathVariable String clientId) {
        var history = getTransactionHistoryUseCase.execute(clientId).stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PatchMapping("/{clientId}/preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<Void> updatePreferences(
            @PathVariable String clientId,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        var client = clientPort.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
        client.setNotificationPreference(request.preference());
        client.setContactInfo(request.contactInfo());
        clientPort.save(client);
        return ResponseEntity.noContent().build();
    }
}
