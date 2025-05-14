package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;

public record GotExtract(@NotBlank String contract, @NotBlank String extract, @NotBlank String hmac, @NotBlank String signature) {
}
