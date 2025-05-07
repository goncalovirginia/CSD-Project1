package csd.client.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HexFormat;

@Service
public class HMACService {

	private final Mac mac;

	public HMACService(@Value("${crypto.hmac.algorithm}") String algorithm, @Value("${crypto.hmac.keyHex}") String key) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		mac = Mac.getInstance(algorithm, "BC");
		mac.init(new SecretKeySpec(HexFormat.of().parseHex(key), algorithm));
	}

	public byte[] hash(byte[] data) {
		return mac.doFinal(data);
	}

	public int getIntegrityLength() {
		return mac.getMacLength();
	}

}
