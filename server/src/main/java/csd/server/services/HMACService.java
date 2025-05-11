package csd.server.services;

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

	private final String algorithm, keyBase64;
	private final Mac mac;

	public HMACService(@Value("${crypto.hmac.algorithm}") String algorithm, @Value("${crypto.hmac.keyBase64}") String key) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		this.algorithm = algorithm;
		this.keyBase64 = key;
		this.mac = Mac.getInstance(algorithm, "BC");
		this.mac.init(new SecretKeySpec(Base64.getDecoder().decode(keyBase64), algorithm));
	}

	public byte[] hash(byte[] data) {
		return mac.doFinal(data);
	}

	private byte[] hash(byte[] data, String keyBase64) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
		Mac mac = Mac.getInstance(this.algorithm, "BC");
		mac.init(new SecretKeySpec(Base64.getDecoder().decode(keyBase64), this.algorithm));
		return mac.doFinal(data);
	}

	public boolean validateHmac(byte[] data, byte[] hmac, String keyBase64) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
		return Arrays.equals(hash(data, keyBase64), hmac);
	}

	public String hashToBase64(byte[] data) {
		return Base64.getEncoder().encodeToString(hash(data));
	}

	public int getIntegrityLength() {
		return mac.getMacLength();
	}

	public String getKeyBase64() {
		return keyBase64;
	}

}
