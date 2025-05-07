package csd.server.controllers.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GetTotalValue(@NotEmpty List<String> contracts, @NotBlank String signature) {
}
