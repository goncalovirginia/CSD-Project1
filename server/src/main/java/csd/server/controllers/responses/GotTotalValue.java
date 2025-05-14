package csd.server.controllers.responses;

import java.util.List;

public record GotTotalValue(List<String> contracts, long totalValue, String hmac, String signature) {
}
