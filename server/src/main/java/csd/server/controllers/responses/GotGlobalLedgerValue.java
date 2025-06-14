package csd.server.controllers.responses;

public record GotGlobalLedgerValue(String contract, long globalLedgerValue, String hmac, String signature) {
}
