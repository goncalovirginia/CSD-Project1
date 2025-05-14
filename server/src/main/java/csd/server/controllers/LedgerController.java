package csd.server.controllers;

import csd.server.controllers.requests.*;
import csd.server.controllers.responses.*;
import csd.server.exceptions.InvalidHmacException;
import csd.server.exceptions.InvalidSignatureException;
import csd.server.exceptions.NonceReplayException;
import csd.server.models.LedgerEntity;
import csd.server.services.DigitalSignatureService;
import csd.server.services.HMACService;
import csd.server.services.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

	private final LedgerService ledgerService;
	private final HMACService hmacService;
	private final DigitalSignatureService digitalSignatureService;
	private final Set<String> nonces;

	public LedgerController(LedgerService ledgerService, HMACService hmacService, DigitalSignatureService digitalSignatureService) {
		this.ledgerService = ledgerService;
		this.hmacService = hmacService;
		this.digitalSignatureService = digitalSignatureService;
		this.nonces = new HashSet<>();
	}

	@PostMapping(path = "/createContract", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CreatedContract> createContract(@RequestBody @Valid CreateContract body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), Base64.getDecoder().decode(body.hmacKey()), Base64.getDecoder().decode(body.publicKey())));
		validateHmac(message, body.hmac(), body.hmacKey());
		validateSignature(message, body.signature(), body.publicKey());

		ledgerService.createContract(body.contract(), body.hmacKey(), body.publicKey());

		byte[] response = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), Base64.getDecoder().decode(digitalSignatureService.getPublicKeyBase64())));
		String hmac = hmacService.hashToBase64(response, body.hmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new CreatedContract(body.contract(), digitalSignatureService.getPublicKeyBase64(), hmac, signature));
	}

	@PostMapping(path = "/loadMoney", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<LoadedMoney> loadMoney(@RequestBody @Valid LoadMoney body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), longToBytes(body.value())));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		ledgerService.loadMoney(body.contract(), body.value());

		byte[] response = message.clone();
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new LoadedMoney(body.contract(), body.value(), hmac, signature));
	}

	@PostMapping(path = "/sendTransaction", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SentTransaction> sendTransaction(@RequestBody @Valid SendTransaction body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		if (!nonces.add(body.nonce()))
			throw new NonceReplayException();

		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.originContract()), Base64.getDecoder().decode(body.destinationContract()), longToBytes(body.value()), body.nonce().getBytes(StandardCharsets.UTF_8)));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.originContract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		ledgerService.sendTransaction(body.originContract(), body.destinationContract(), body.value());

		byte[] response = message.clone();
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new SentTransaction(body.originContract(), body.destinationContract(), body.value(), body.nonce(), hmac, signature));
	}

	@PostMapping(path = "/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GotBalance> getBalance(@RequestBody @Valid GetBalance body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract())));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		long balance = ledgerService.getBalance(body.contract());

		byte[] response = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), longToBytes(balance)));
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new GotBalance(body.contract(), balance, hmac, signature));
	}

	@PostMapping(path = "/getExtract", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GotExtract> getExtract(@RequestBody @Valid GetExtract body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract())));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		String extract = ledgerService.getExtract(body.contract());

		byte[] response = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), Base64.getDecoder().decode(extract)));
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new GotExtract(body.contract(), extract, hmac, signature));
	}

	@PostMapping(path = "/getTotalValue", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GotTotalValue> getTotalValue(@RequestBody @Valid GetTotalValue body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(body.contracts().stream().map(c -> Base64.getDecoder().decode(c)).toList());
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contracts().getFirst());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		long totalValue = ledgerService.getTotalValue(body.contracts());

		byte[] response = appendByteArrays(List.of(message, longToBytes(totalValue)));
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new GotTotalValue(body.contracts(), totalValue, hmac, signature));
	}

	@PostMapping(path = "/getGlobalLedgerValue", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GotGlobalLedgerValue> getGlobalLedgerValue(@RequestBody @Valid GetGlobalLedgerValue body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract())));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		long globalLedgerValue = ledgerService.getGlobalLedgerValue();

		byte[] response = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract()), longToBytes(globalLedgerValue)));
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new GotGlobalLedgerValue(body.contract(), globalLedgerValue, hmac, signature));
	}

	@PostMapping(path = "/getLedger", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GotLedger> getLedger(@RequestBody @Valid GetLedger body) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(body.contract())));
		LedgerEntity ledgerEntity = ledgerService.getLedgerEntity(body.contract());
		validateHmac(message, body.hmac(), ledgerEntity.getHmacKey());
		validateSignature(message, body.signature(), ledgerEntity.getPublicKey());

		List<String> ledger = ledgerService.getLedger();

		byte[] response = appendByteArrays(List.of(message, appendByteArrays(ledger.stream().map(line -> Base64.getDecoder().decode(line)).toList())));
		String hmac = hmacService.hashToBase64(response, ledgerEntity.getHmacKey());
		String signature = digitalSignatureService.signToBase64(response);

		return ResponseEntity.ok(new GotLedger(body.contract(), ledger, hmac, signature));
	}

	private void validateHmac(byte[] message, String hmac, String hmacKey) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
		if (!hmacService.validateHmac(message, Base64.getDecoder().decode(hmac), hmacKey))
			throw new InvalidHmacException();
	}

	private void validateSignature(byte[] message, String signature, String publicKey) {
		byte[] signatureBytes = Base64.getDecoder().decode(signature);
		byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);

		if (!DigitalSignatureService.validateSignature(message, signatureBytes, publicKeyBytes))
			throw new InvalidSignatureException();
	}

	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	private byte[] appendByteArrays(List<byte[]> fields) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		for (byte[] field : fields)
			byteArrayOutputStream.write(field);
		return byteArrayOutputStream.toByteArray();
	}

}
