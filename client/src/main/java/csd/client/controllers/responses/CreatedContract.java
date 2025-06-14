package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;

public record CreatedContract(@NotBlank String contract, @NotBlank String hmac, @NotBlank String signature) {
}
