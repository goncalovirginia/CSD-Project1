package csd.client.controllers.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record GotTotalValue(@NotEmpty List<String> contracts, @Positive long totalValue, @NotBlank String hmac, @NotBlank String signature) {
}
