package csd.client.services;

import csd.client.controllers.requests.CreateContract;
import csd.client.controllers.requests.LoadMoney;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public void loadMoney(String contract, int value) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byteArrayOutputStream.write(Base64.getDecoder().decode(contract));
		byteArrayOutputStream.write(value);
		String message = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());

		String signature = digitalSignatureService.signBase64(message, contractsKeysMap.get(contract).getFirst());
		restClient.post().uri(ledgerAddress + "/ledger/loadMoney").body(new LoadMoney(contract, value, signature)).retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();
	}

}
