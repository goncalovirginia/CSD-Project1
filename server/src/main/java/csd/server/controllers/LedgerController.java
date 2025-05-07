package csd.server.controllers;

import csd.server.controllers.requests.CreateContract;
import csd.server.controllers.requests.LoadMoney;
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
import java.util.Base64;

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
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(Base64.getDecoder().decode(body.contract()));
		byteArrayOutputStream.write((int) body.value());
		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());

		getPublicKeyAndValidateSignature(message, body.signature(), body.contract());
		ledgerService.loadMoney(body.contract(), body.value());
		return ResponseEntity.noContent().build();
	}

	@PostMapping(path = "/sendTransaction")
	public ResponseEntity<String> sendTransaction() {
		return ResponseEntity.ok("");
	}

	@GetMapping(path = "/getBalance")
	public ResponseEntity<String> getBalance() {
		return ResponseEntity.ok("");
	}

	@GetMapping(path = "/getExtract")
	public ResponseEntity<String> getExtract() {
		return ResponseEntity.ok("");
	}

	@GetMapping(path = "/getTotalValue")
	public ResponseEntity<String> getTotalValue() {
		return ResponseEntity.ok("");
	}

	@GetMapping(path = "/getGlobalLedgerValue")
	public ResponseEntity<String> getGlobalLedgerValue() {
		return ResponseEntity.ok("");
	}

	@GetMapping(path = "/getLedger")
	public ResponseEntity<String> getLedger() {
		return ResponseEntity.ok("");
	}

	private void validateSignature(String content, String signature, String publicKey) {
		byte[] contentBytes = Base64.getDecoder().decode(content);
		byte[] signatureBytes = Base64.getDecoder().decode(signature);
		byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);

		if (!DigitalSignatureService.validateSignature(contentBytes, signatureBytes, publicKeyBytes))
			throw new InvalidSignatureException();
	}

	private void getPublicKeyAndValidateSignature(String content, String signature, String contract) {
		String publicKey = ledgerService.getContractPublicKey(contract);
		validateSignature(content, signature, publicKey);
	}

}
