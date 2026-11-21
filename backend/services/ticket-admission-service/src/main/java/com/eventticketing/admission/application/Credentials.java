package com.eventticketing.admission.application;
import com.eventticketing.platform.SecretCipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class Credentials {
 private final SecretCipher cipher;private final SecureRandom random=new SecureRandom();
 public Credentials(@Value("${app.data-encryption-key}")String key){cipher=new SecretCipher(key);}
 public String create(){byte[] b=new byte[32];random.nextBytes(b);return "ETP1."+Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 public String encrypt(UUID id,String raw){return cipher.encrypt(raw,"ticket:"+id);}
 public String decrypt(UUID id,String value){return cipher.decrypt(value,"ticket:"+id);}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
}
