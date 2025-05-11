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
	private final HMACService hmacService;
	private final String ledgerAddress;

	private final File contractsKeysFile;
	private final Map<String, List<String>> contractsKeysMap;

	public RestClientService(RestClient restClient, HMACService hmacService, DigitalSignatureService digitalSignatureService, ResourceLoader resourceLoader, @Value("${ledger.address}") String ledgerAddress) throws IOException {
		this.restClient = restClient;
		this.hmacService = hmacService;
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

		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(contract), Base64.getDecoder().decode(hmacService.getKeyBase64()), Base64.getDecoder().decode(publicKeyBase64)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(contract, message);
		restClient.post().uri(ledgerAddress + "/ledger/createContract").body(new CreateContract(contract, hmacService.getKeyBase64(), publicKeyBase64, hmac, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public void loadMoney(String contract, long value) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(contract), longToBytes(value)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(contract, message);
		restClient.post().uri(ledgerAddress + "/ledger/loadMoney").body(new LoadMoney(contract, value, hmac, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public void sendTransaction(String originContract, String destinationContract, Long value) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String nonce = UUID.randomUUID().toString();
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(originContract), Base64.getDecoder().decode(destinationContract), longToBytes(value), nonce.getBytes(StandardCharsets.UTF_8)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(originContract, message);
		restClient.post().uri(ledgerAddress + "/ledger/sendTransaction").body(new SendTransaction(originContract, destinationContract, value, nonce, hmac, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

	public long getBalance(String contract) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(contract)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(contract, message);
		return restClient.post().uri(ledgerAddress + "/ledger/getBalance").body(new GetBalance(contract, hmac, signature)).retrieve().body(Long.class);
	}

	public String getExtract(String contract) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(contract)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(contract, message);
		return restClient.post().uri(ledgerAddress + "/ledger/getExtract").body(new GetExtract(contract, hmac, signature)).retrieve().body(String.class);
	}

	public long getTotalValue(List<String> contracts) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		byte[] message = appendByteArrays(contracts.stream().map(c -> Base64.getDecoder().decode(c)).toList());
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(contracts.getFirst(), message);
		return restClient.post().uri(ledgerAddress + "/ledger/getTotalValue").body(new GetTotalValue(contracts, hmac, signature)).retrieve().body(Long.class);
	}

	public long getGlobalLedgerValue() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String firstContract = contractsKeysMap.keySet().stream().toList().getFirst();
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(firstContract)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(firstContract, message);
		return restClient.post().uri(ledgerAddress + "/ledger/getGlobalLedgerValue").body(new GetGlobalLedgerValue(firstContract, hmac, signature)).retrieve().body(Long.class);
	}

	public List<String> getLedger() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		String firstContract = contractsKeysMap.keySet().stream().toList().getFirst();
		byte[] message = appendByteArrays(List.of(Base64.getDecoder().decode(firstContract)));
		String hmac = hmacService.hashToBase64(message);
		String signature = buildSignature(firstContract, message);
		return restClient.post().uri(ledgerAddress + "/ledger/getLedger").body(new GetLedger(firstContract, hmac, signature)).retrieve().body(List.class);
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

	private String buildSignature(String contract, byte[] message) throws NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, NoSuchProviderException, InvalidKeyException {
		return DigitalSignatureService.signToBase64(message, contractsKeysMap.get(contract).getFirst());
	}

}
