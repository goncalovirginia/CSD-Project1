package csd.server.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record GetLedger(@NotBlank String contract, @NotBlank String hmac, @NotBlank String signature) {
}
