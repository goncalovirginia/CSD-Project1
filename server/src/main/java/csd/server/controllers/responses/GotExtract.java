package csd.server.controllers.responses;

public record GotExtract(String contract, String extract, String hmac, String signature) {
}
