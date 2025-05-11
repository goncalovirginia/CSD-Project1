package csd.client.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateContract(@NotBlank String contract, @NotBlank String hmacKey, @NotBlank String publicKey, @NotBlank String hmac, @NotBlank String signature) {
}
