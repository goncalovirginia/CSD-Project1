package csd.server.controllers.requests;

import jakarta.validation.constraints.NotBlank;

public record GetBalance(@NotBlank String contract, @NotBlank String signature) {
}
