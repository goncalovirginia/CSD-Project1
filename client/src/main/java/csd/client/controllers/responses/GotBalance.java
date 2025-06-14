package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GotBalance(@NotBlank String contract, @Positive long balance, @NotBlank String hmac, @NotBlank String signature) {
}
