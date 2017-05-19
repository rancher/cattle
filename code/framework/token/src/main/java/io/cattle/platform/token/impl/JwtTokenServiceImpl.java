package io.cattle.platform.token.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.token.TokenDecryptionException;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class JwtTokenServiceImpl implements TokenService {

    private static final String KEY_ID = "kid";

    private static final DynamicStringProperty SUBJECT = ArchaiusUtil.getString("jwt.default.subject");
    private static final DynamicStringProperty ISSUER = ArchaiusUtil.getString("jwt.default.issuer");
    private static final DynamicLongProperty EXPIRATION = ArchaiusUtil.getLong("jwt.default.expiration.seconds");

    RSAKeyProvider keyProvider;

    @Override
    public String generateToken(Map<String, Object> payload) {
        return generateToken(payload, new Date(), new Date(System.currentTimeMillis() + EXPIRATION.get() * 1000), false);
    }

    @Override
    public String generateToken(Map<String, Object> payload, Date expireDate) {
        return generateToken(payload, new Date(), expireDate, false);
    }

    @Override
    public String generateEncryptedToken(Map<String, Object> payload) {
        return generateToken(payload, new Date(), new Date(System.currentTimeMillis() + EXPIRATION.get() * 1000), true);
    }

    @Override
    public String generateEncryptedToken(Map<String, Object> payload, Date expireDate) {
        return generateToken(payload, new Date(), expireDate, true);
    }

    protected String generateToken(Map<String, Object> payload, Date issueTime, Date expireDate, boolean encrypted) {

        // Prepare JWT with claims set
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

        if (payload != null) {
            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                builder.claim(entry.getKey(), entry.getValue());
            }
        }

        if (expireDate != null) {
            builder.expirationTime(expireDate);
        }

        builder.subject(SUBJECT.get());
        builder.issueTime(issueTime);
        builder.issuer(ISSUER.get());

        if (encrypted) {
            JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128GCM);
            EncryptedJWT jwt = new EncryptedJWT(header, builder.build());
            RSAEncrypter encrypter = new RSAEncrypter((RSAPublicKey) keyProvider.getDefaultPublicKey());
            try {
                jwt.encrypt(encrypter);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to generate encrypted token", e);
            }
            return jwt.serialize();
        } else {
            RSAPrivateKeyHolder privateKey = keyProvider.getPrivateKey();

            builder.claim(KEY_ID, privateKey.getKeyId());
            // Create RSA-signer with the private key
            JWSSigner signer = new RSASSASigner(privateKey.getKey());

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), builder.build());

            // Compute the RSA signature
            try {
                signedJWT.sign(signer);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to generate token", e);
            }

            return signedJWT.serialize();
        }
    }

    public RSAKeyProvider getKeyProvider() {
        return keyProvider;
    }

    @Override
    public Map<String, Object> getJsonPayload(String token, boolean encrypted) throws TokenException {
        if (StringUtils.isEmpty(token)) {
            throw new TokenException("null or empty token");
        }
        if (encrypted) {
            EncryptedJWT jwt = null;
            try {
                jwt = EncryptedJWT.parse(token);
                RSADecrypter decrypter = new RSADecrypter(keyProvider.getPrivateKey().getKey());
                jwt.decrypt(decrypter);
            } catch (JOSEException | ParseException e) {
                throw new TokenDecryptionException("Invalid token", e);
            }
            return getJSONObject(jwt, encrypted);
        }
        try {
            JWSObject jws = JWSObject.parse(token);
            JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) keyProvider.getDefaultPublicKey());
            if (!jws.verify(verifier)) {
                throw new TokenException("ERROR: Fradulent token");
            }
            return getJSONObject(jws, encrypted);
        } catch (TokenException | ParseException | JOSEException e) {
            throw new TokenException("Error: Fradulent token, unrecognized signature", e);
        }
    }

    private Map<String, Object> getJSONObject(JOSEObject jose, boolean encrypted) throws TokenException {
        Long exp = (Long) jose.getPayload().toJSONObject().get("exp");
        if (exp != null && exp * 1000 <= System.currentTimeMillis()) {
            throw new TokenException("Expired Token");
        }
        return jose.getPayload().toJSONObject();
    }

    @Inject
    public void setKeyProvider(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

}