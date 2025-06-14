package csd.client.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Service
public class HashService {

	private final MessageDigest messageDigest;

	public HashService(@Value("${crypto.hash.algorithm}") String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException {
		messageDigest = MessageDigest.getInstance(algorithm, "BC");
	}

	public byte[] hash(byte[] data) {
		return messageDigest.digest(data);
	}

	public int getIntegrityLength() {
		return messageDigest.getDigestLength();
	}

}
