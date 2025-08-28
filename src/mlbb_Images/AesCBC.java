package mlbb_Images;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.ByteBuffer;

public final class AesCBC {
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH_BIT = 128;
	private static final SecureRandom RNG = new SecureRandom();

	// Suppresses default constructor, ensuring non-instantiability.
	private AesCBC() {}
	
	/** Generates a random AES key with {@code bits} bits. 
	 * @throws NoSuchAlgorithmException */
	public static SecretKey newAesKey(int bits) throws NoSuchAlgorithmException {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(bits);
		return kg.generateKey();
	}
	
	/** Generates a random 128-bit IV. */
	public static byte[] randomIV() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return iv;
	}
	
	/** Generates a byte array from a hex string. */
	public static byte[] fromHex(String s) {
		int len = s.length();
		if (len != 32) {
			throw new IllegalArgumentException("hex string length is not 32");
		}
		byte[] out = new byte[len / 2];
		for (int i = 0; i < out.length; i++) {
			int hi = Character.digit(s.charAt(2 * i), 16);
			int lo = Character.digit(s.charAt(2 * i + 1), 16);
			out[i] = (byte)((hi << 4) | lo);
		}
		return out;
	}
	
	/** Output layout: [IV(16) | PKCS5 ciphertext]. 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException */
	public static byte[] aesCbcEncryptPkcs5(byte[] plaintext, SecretKey key, boolean randomIV)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		byte[] iv = randomIV ? randomIV() : AesCBC.fromHex("000102030405060708090a0b0c0d0e0f");
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		byte[] ct = c.doFinal(plaintext);
		ByteBuffer out = ByteBuffer.allocate(16 + ct.length);
		out.put(iv).put(ct);
		return out.array();
	}
	
	/** Input layout: [IV(16) | PKCS5 ciphertext]. 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidAlgorithmParameterException 
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException
	 * */
	static byte[] aesCbcDecryptPkcs5(byte[] blob, SecretKey key)
			throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		if (blob.length < 16) {
			throw new IllegalArgumentException("too short");
		}
		byte[] iv = new byte[16];
		byte[] ct = new byte[blob.length - 16];
		System.arraycopy(blob, 0, iv, 0, 16);
		System.arraycopy(blob, 16, ct, 0, ct.length);
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		return c.doFinal(ct);
	}
	
	/**
	 * Encrypt a byte[] with AES-GCM.
	 * Output layout: [IV (12 bytes) | ciphertext+tag (... bytes)]
	 * AAD may be null (optional associated data for authenticity, not encrypted).
	 */
	public static byte[] aesGcmEncrypt(byte[] plaintext, SecretKey key, byte[] aad)
			throws GeneralSecurityException {
		byte[] iv = new byte[IV_LENGTH];
		RNG.nextBytes(iv);
		
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);
		if (aad != null) cipher.updateAAD(aad);
		
		byte[] ctAndTag = cipher.doFinal(plaintext);
		
		ByteBuffer out = ByteBuffer.allocate(iv.length + ctAndTag.length);
		out.put(iv).put(ctAndTag);
		return out.array();
	}
	
	/** Decrypt data produced by encrypt(); returns plaintext or throws AEADBadTagException on tamper. */
	public static byte[] aesGcmDecrypt(byte[] ivAndCiphertext, SecretKey key, byte[] aad)
			throws GeneralSecurityException {
		ByteBuffer in = ByteBuffer.wrap(ivAndCiphertext);
		
		byte[] iv = new byte[IV_LENGTH];
		in.get(iv);
		
		byte[] ctAndTag = new byte[in.remaining()];
		in.get(ctAndTag);
		
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, spec);
		if (aad != null) cipher.updateAAD(aad);
		
		return cipher.doFinal(ctAndTag);
	}
}
