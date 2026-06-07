package com.harrified.finCore.iam.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
public class JwtKeyProvider {

    @Value("${jwt.private-key:}")
    private String privateKeyPem;

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    private RSAKey rsaKey;

    @PostConstruct
    public void init() throws Exception {
        if (privateKeyPem.isBlank()) {
            // Dev mode: generate an ephemeral key pair on startup.
            // In prod, set JWT_PRIVATE_KEY + JWT_PUBLIC_KEY env vars.
            rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .generate();
        } else {
            rsaKey = loadFromPem();
        }
    }

    public RSAPublicKey getPublicKey() throws Exception {
        return rsaKey.toRSAPublicKey();
    }

    public JWKSet getJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    private RSAKey loadFromPem() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        // PEM env vars may use literal \n — normalise both cases.
        String normalizedPrivate = stripPem(privateKeyPem, "PRIVATE KEY");
        byte[] privateBytes = Base64.getDecoder().decode(normalizedPrivate);
        RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

        String normalizedPublic = stripPem(publicKeyPem, "PUBLIC KEY");
        byte[] publicBytes = Base64.getDecoder().decode(normalizedPublic);
        RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(publicBytes));

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("fincore-iam-key")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    private String stripPem(String pem, String label) {
        return pem.replace("\\n", "\n")
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
    }
}
