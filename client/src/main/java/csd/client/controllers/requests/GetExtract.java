package csd.client.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record GetExtract(@NotBlank String contract, @NotBlank String hmac, @NotBlank String signature) {
}
