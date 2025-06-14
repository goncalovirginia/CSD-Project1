package csd.client.controllers.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SendTransaction(@NotBlank String originContract, @NotBlank String destinationContract, @Positive long value, @NotBlank String nonce, @NotBlank String hmac, @NotBlank String signature) {
}
