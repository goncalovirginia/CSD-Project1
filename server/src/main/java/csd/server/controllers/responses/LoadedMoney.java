package csd.server.controllers.responses;

public record LoadedMoney(String contract, long balance, String hmac, String signature) {
}
