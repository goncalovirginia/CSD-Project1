package csd.client.controllers.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LoadMoney(@NotBlank String contract, @Positive long value, @NotBlank String hmac, @NotBlank String signature) {
}
