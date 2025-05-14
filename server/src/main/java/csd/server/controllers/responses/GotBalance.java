package csd.server.controllers.responses;

public record GotBalance(String contract, long balance, String hmac, String signature) {
}
