package csd.server.controllers;

import csd.server.controllers.requests.*;
import csd.server.controllers.responses.CreatedContract;
import csd.server.exceptions.InvalidSignatureException;
import csd.server.services.DigitalSignatureService;
import csd.server.services.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

	private final LedgerService ledgerService;

	public LedgerController(LedgerService ledgerService, DigitalSignatureService digitalSignatureService) {
		this.ledgerService = ledgerService;
	}

	@PostMapping(path = "/createContract", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CreatedContract> createContract(@RequestBody @Valid CreateContract body) {
		validateSignature(body.contract(), body.signature(), body.publicKey());

		ledgerService.createContract(body.contract(), body.publicKey());
		return ResponseEntity.ok(new CreatedContract(body.contract()));
	}

	@PostMapping(path = "/loadMoney", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> loadMoney(@RequestBody @Valid LoadMoney body) throws IOException {
		joinPartsAndValidateSignature(body.contract(), body.signature(),
			Base64.getDecoder().decode(body.contract()), longToBytes(body.value()));

		ledgerService.loadMoney(body.contract(), body.value());
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/sendTransaction", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> sendTransaction(@RequestBody @Valid SendTransaction body) throws IOException {
		joinPartsAndValidateSignature(body.originContract(), body.signature(),
			Base64.getDecoder().decode(body.originContract()), Base64.getDecoder().decode(body.destinationContract()), longToBytes(body.value()), body.nonce().getBytes(StandardCharsets.UTF_8));

		ledgerService.sendTransaction(body.originContract(), body.destinationContract(), body.value());
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/getBalance", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> getBalance(@RequestBody @Valid GetBalance body) throws IOException {
		joinPartsAndValidateSignature(body.contract(), body.signature(),
			Base64.getDecoder().decode(body.contract()));

		long balance = ledgerService.getBalance(body.contract());
		return ResponseEntity.ok(balance);
	}

	@PostMapping(path = "/getExtract", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> getExtract(@RequestBody @Valid GetExtract body) throws IOException {
		joinPartsAndValidateSignature(body.contract(), body.signature(),
			Base64.getDecoder().decode(body.contract()));

		String extract = ledgerService.getExtract(body.contract());
		return ResponseEntity.ok(extract);
	}

	@PostMapping(path = "/getTotalValue", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> getTotalValue(@RequestBody @Valid GetTotalValue body) throws IOException {
		joinPartsAndValidateSignature(body.contracts().getFirst(), body.signature(),
			body.contracts().stream().map(c -> Base64.getDecoder().decode(c)).toList());

		long totalValue = ledgerService.getTotalValue(body.contracts());
		return ResponseEntity.ok(totalValue);
	}

	@PostMapping(path = "/getGlobalLedgerValue", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Long> getGlobalLedgerValue(@RequestBody @Valid GetGlobalLedgerValue body) throws IOException {
		joinPartsAndValidateSignature(body.contract(), body.signature(),
			Base64.getDecoder().decode(body.contract()));

		long globalLedgerValue = ledgerService.getGlobalLedgerValue();
		return ResponseEntity.ok(globalLedgerValue);
	}

	@PostMapping(path = "/getLedger", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> getLedger(@RequestBody @Valid GetLedger body) throws IOException {
		joinPartsAndValidateSignature(body.contract(), body.signature(),
			Base64.getDecoder().decode(body.contract()));

		List<String> ledger = ledgerService.getLedger();
		return ResponseEntity.ok(ledger);
	}

	private void joinPartsAndValidateSignature(String contract, String signature, byte[]... parts) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		for (byte[] part : parts)
			byteArrayOutputStream.write(part);
		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
		getPublicKeyAndValidateSignature(message, signature, contract);
	}

	private void joinPartsAndValidateSignature(String contract, String signature, List<byte[]> parts) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		for (byte[] part : parts)
			byteArrayOutputStream.write(part);
		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
		getPublicKeyAndValidateSignature(message, signature, contract);
	}

	private void getPublicKeyAndValidateSignature(String content, String signature, String contract) {
		String publicKey = ledgerService.getContractPublicKey(contract);
		validateSignature(content, signature, publicKey);
	}

	private void validateSignature(String content, String signature, String publicKey) {
		byte[] contentBytes = Base64.getDecoder().decode(content);
		byte[] signatureBytes = Base64.getDecoder().decode(signature);
		byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);

		if (!DigitalSignatureService.validateSignature(contentBytes, signatureBytes, publicKeyBytes))
			throw new InvalidSignatureException();
	}

	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

}
