package csd.client.services;

import csd.client.controllers.requests.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

@Service
public class RestClientService {

	private final RestClient restClient;
	private final DigitalSignatureService digitalSignatureService;
	private final ResourceLoader resourceLoader;
	private final String ledgerAddress;

	private final File contractsKeysFile;
	private final Map<String, List<String>> contractsKeysMap;

	public RestClientService(RestClient restClient, DigitalSignatureService digitalSignatureService, ResourceLoader resourceLoader, @Value("${ledger.address}") String ledgerAddress) throws IOException {
		this.restClient = restClient;
		this.digitalSignatureService = digitalSignatureService;
		this.resourceLoader = resourceLoader;
		this.ledgerAddress = ledgerAddress;

		this.contractsKeysFile = ResourceUtils.getFile("classpath:contract-public-private-keys.txt");
		List<String> contractsKeysList = Files.readAllLines(contractsKeysFile.toPath());
		contractsKeysMap = new HashMap<>();
		for (String contractKeys : contractsKeysList) {
			String[] parts = contractKeys.split(":");
			contractsKeysMap.put(parts[0], List.of(parts[1], parts[2]));
		}
	}

	public void createContract(String contract) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
		KeyPair keyPair = DigitalSignatureService.generateKeyPair();
		if (keyPair == null) return;

		String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
		String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

		System.out.println("New EC KeyPair generated:");
		System.out.println("Private key: " + privateKeyBase64);
		System.out.println("Public key: " + publicKeyBase64);

		Files.write(contractsKeysFile.toPath(), List.of(contract + ":" + privateKeyBase64 + ":" + publicKeyBase64), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		contractsKeysMap.put(contract, List.of(privateKeyBase64, publicKeyBase64));

		String signature = digitalSignatureService.signBase64(contract, privateKeyBase64);

		restClient.post().uri(ledgerAddress + "/ledger/createContract").body(new CreateContract(contract, publicKeyBase64, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public void loadMoney(String contract, long value) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
		String signature = buildSignature(contract, Base64.getDecoder().decode(contract), longToBytes(value));
		restClient.post().uri(ledgerAddress + "/ledger/loadMoney").body(new LoadMoney(contract, value, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public void sendTransaction(String originContract, String destinationContract, Long value) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String nonce = UUID.randomUUID().toString();
		String signature = buildSignature(originContract, Base64.getDecoder().decode(originContract), Base64.getDecoder().decode(destinationContract), longToBytes(value), nonce.getBytes(StandardCharsets.UTF_8));
		restClient.post().uri(ledgerAddress + "/ledger/sendTransaction").body(new SendTransaction(originContract, destinationContract, value, nonce, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public long getBalance(String contract) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String signature = buildSignature(contract, Base64.getDecoder().decode(contract));
		return restClient.post().uri(ledgerAddress + "/ledger/getBalance").body(new GetBalance(contract, signature)).retrieve().body(Long.class);
	}

	public String getExtract(String contract) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String signature = buildSignature(contract, Base64.getDecoder().decode(contract));
		return restClient.post().uri(ledgerAddress + "/ledger/getExtract").body(new GetExtract(contract, signature)).retrieve().body(String.class);
	}

	public long getTotalValue(List<String> contracts) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String signature = buildSignature(contracts.getFirst(), contracts.stream().map(c -> Base64.getDecoder().decode(c)).toList());
		return restClient.post().uri(ledgerAddress + "/ledger/getTotalValue").body(new GetTotalValue(contracts, signature)).retrieve().body(Long.class);
	}

	public long getGlobalLedgerValue() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String firstContract = contractsKeysMap.keySet().stream().toList().getFirst();
		String signature = buildSignature(firstContract, Base64.getDecoder().decode(firstContract));
		return restClient.post().uri(ledgerAddress + "/ledger/getGlobalLedgerValue").body(new GetGlobalLedgerValue(firstContract, signature)).retrieve().body(Long.class);
	}

	public List<String> getLedger() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String firstContract = contractsKeysMap.keySet().stream().toList().getFirst();
		String signature = buildSignature(firstContract, Base64.getDecoder().decode(firstContract));
		return restClient.post().uri(ledgerAddress + "/ledger/getLedger").body(new GetLedger(firstContract, signature)).retrieve().body(List.class);
	}

	private byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	private String buildSignature(String contract, byte[]... fields) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		for (byte[] field : fields)
			byteArrayOutputStream.write(field);

		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
		return digitalSignatureService.signBase64(message, contractsKeysMap.get(contract).getFirst());
	}

	private String buildSignature(String contract, List<byte[]> fields) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		for (byte[] field : fields)
			byteArrayOutputStream.write(field);

		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
		return digitalSignatureService.signBase64(message, contractsKeysMap.get(contract).getFirst());
	}

}
