package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GotGlobalLedgerValue(@NotBlank String contract, @Positive long globalLedgerValue, @NotBlank String hmac, @NotBlank String signature) {
}
