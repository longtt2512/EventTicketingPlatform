package com.eventticketing.identity.security;

import com.eventticketing.platform.SecretCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SecretConfiguration {
    @Bean SecretCipher secretCipher(@Value("${app.data-encryption-key}") String key) { return new SecretCipher(key); }
}
