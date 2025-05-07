package csd.server.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateContract(@NotBlank String contract, @NotBlank String publicKey, @NotBlank String signature) {
}
