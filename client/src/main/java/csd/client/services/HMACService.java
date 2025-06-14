package csd.client.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class HMACService {

	private final String keyBase64;
	private final Mac mac;

	public HMACService(@Value("${crypto.hmac.algorithm}") String algorithm, @Value("${crypto.hmac.keyBase64}") String key) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		keyBase64 = key;
		mac = Mac.getInstance(algorithm, "BC");
		mac.init(new SecretKeySpec(Base64.getDecoder().decode(keyBase64), algorithm));
	}

	public byte[] hash(byte[] data) {
		return mac.doFinal(data);
	}

	public String hashToBase64(byte[] data) {
		return Base64.getEncoder().encodeToString(hash(data));
	}

	public boolean validateHmac(byte[] data, byte[] hmac) {
		return Arrays.equals(hash(data), hmac);
	}

	public int getIntegrityLength() {
		return mac.getMacLength();
	}

	public String getKeyBase64() {
		return keyBase64;
	}

}
