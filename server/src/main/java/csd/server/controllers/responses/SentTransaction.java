package csd.server.controllers.responses;

public record SentTransaction(String originContract, String destinationContract, long value, String nonce, String hmac, String signature) {
}
