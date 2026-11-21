package com.eventticketing.identity.api;

import com.eventticketing.identity.security.JwtKeyConfiguration;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/.well-known")
public class JwkController {
    private final RSAKey rsaKey;

    public JwkController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/jwks.json")
    public Object keys() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
