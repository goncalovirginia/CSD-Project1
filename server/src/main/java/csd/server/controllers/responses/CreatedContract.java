package csd.server.controllers.responses;

public record CreatedContract(String contract, String hmac, String signature) {
}
