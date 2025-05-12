package csd.server.controllers.responses;

public record CreatedContract(String contract, String publicKey, String hmac, String signature) {
}
