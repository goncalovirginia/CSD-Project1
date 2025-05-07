package csd.client.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record GetGlobalLedgerValue(@NotBlank String contract, @NotBlank String signature) {
}
