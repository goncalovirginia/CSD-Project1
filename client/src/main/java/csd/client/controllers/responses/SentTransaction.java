package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SentTransaction(@NotBlank String originContract, @NotBlank String destinationContract, @Positive long value, @NotBlank String nonce, @NotBlank String hmac, @NotBlank String signature) {
}
