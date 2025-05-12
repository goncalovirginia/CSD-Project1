package csd.server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class DigitalSignatureService {

	private final PrivateKey privateKey;
	private final String publicKeyBase64;
	private final PublicKey publicKey;
	private final Signature signature;

	public DigitalSignatureService(@Value("${crypto.dsa.privateKeyBase64}") String privateKeyBase64, @Value("${crypto.dsa.publicKeyBase64}") String publicKeyBase64) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
		KeyFactory kf = KeyFactory.getInstance("EC", "BC");
		privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
		this.publicKeyBase64 = publicKeyBase64;
		publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
		signature = Signature.getInstance("SHA256withECDSA", "BC");
	}

	public static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
			ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256r1");
			keyPairGenerator.initialize(ecGenParameterSpec);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean validateSignature(byte[] messageBytes, byte[] signatureBytes, byte[] senderPublicKeyBytes) {
		try {
			KeyFactory kf = KeyFactory.getInstance("EC", "BC");
			PublicKey senderPublicKey = kf.generatePublic(new X509EncodedKeySpec(senderPublicKeyBytes));
			Signature signature = Signature.getInstance("SHA256withECDSA", "BC");

			signature.initVerify(senderPublicKey);
			signature.update(messageBytes);
			return signature.verify(signatureBytes);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void main(String[] args) {
		KeyPair keyPair = generateKeyPair();
		if (keyPair == null) return;
		System.out.println("Private key: " + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
		System.out.println("Public key: " + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
	}

	public String getPublicKeyBase64() {
		return publicKeyBase64;
	}

	public byte[] sign(byte[] messageBytes) throws InvalidKeyException, SignatureException {
		signature.initSign(privateKey, new SecureRandom());
		signature.update(messageBytes);
		return signature.sign();
	}

	public String signToBase64(byte[] messageBytes) throws InvalidKeyException, SignatureException {
		signature.initSign(privateKey, new SecureRandom());
		signature.update(messageBytes);
		return Base64.getEncoder().encodeToString(signature.sign());
	}

	public boolean validateSignature(byte[] messageBytes, byte[] signatureBytes) throws InvalidKeyException, SignatureException {
		signature.initVerify(publicKey);
		signature.update(messageBytes);
		return signature.verify(signatureBytes);
	}

}
