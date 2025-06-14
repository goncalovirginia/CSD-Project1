package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GotLedger(@NotBlank String contract, @NotEmpty List<String> ledger, @NotBlank String hmac, @NotBlank String signature) {
}
