package csd.server.controllers.responses;

import java.util.List;

public record GotLedger(String contract, List<String> ledger, String hmac, String signature) {
}
