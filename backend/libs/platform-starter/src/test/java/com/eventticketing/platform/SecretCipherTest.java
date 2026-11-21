package com.eventticketing.platform;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Base64;

class SecretCipherTest {
    final SecretCipher cipher = new SecretCipher(Base64.getEncoder().encodeToString(new byte[32]));
    @Test void secretsRoundTripOnlyInTheirOwningContext() {
        String encrypted = cipher.encrypt("private-account", "row-1");
        assertThat(encrypted).doesNotContain("private-account");
        assertThat(cipher.decrypt(encrypted, "row-1")).isEqualTo("private-account");
        assertThatThrownBy(() -> cipher.decrypt(encrypted, "row-2")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void tamperedCiphertextIsRejected() {
        byte[] encrypted = Base64.getDecoder().decode(cipher.encrypt("secret", "row"));
        encrypted[encrypted.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(encrypted), "row")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void missingKeyFailsClosed() { assertThatThrownBy(() -> new SecretCipher("")).isInstanceOf(IllegalArgumentException.class); }
}
