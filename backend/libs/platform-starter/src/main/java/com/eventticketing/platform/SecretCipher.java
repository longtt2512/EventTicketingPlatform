package com.eventticketing.platform;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Authenticated encryption for service-owned secrets; context prevents row swapping. */
public final class SecretCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    public SecretCipher(String base64Key) {
        byte[] bytes = Base64.getDecoder().decode(base64Key);
        if (bytes.length != 32) throw new IllegalArgumentException("DATA_ENCRYPTION_KEY must contain 32 base64-encoded bytes.");
        key = new SecretKeySpec(bytes, "AES");
    }
    public String encrypt(String value, String context) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException("Encryption unavailable", ex); }
    }
    public String decrypt(String value, String context) {
        try {
            var buffer = ByteBuffer.wrap(Base64.getDecoder().decode(value));
            byte[] nonce = new byte[12]; buffer.get(nonce); byte[] encrypted = new byte[buffer.remaining()]; buffer.get(encrypted);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (java.security.GeneralSecurityException | RuntimeException ex) { throw new IllegalArgumentException("Encrypted data cannot be authenticated.", ex); }
    }
}
