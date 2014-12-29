package io.cattle.platform.token.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.token.TokenService;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import net.minidev.json.JSONObject;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
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

    private static JWSVerifier verifier;
    private static RSADecrypter decrypter;

    RSAKeyProvider keyProvider;

    @Override
    public String generateToken(Map<String, Object> payload) {
        return generateToken(payload, new Date(), new Date(System.currentTimeMillis() + EXPIRATION.get() * 1000), false);
    }

    @Override
    public String generateEncryptedToken(Map<String, Object> payload) {
        return generateToken(payload, new Date(), new Date(System.currentTimeMillis() + EXPIRATION.get() * 1000), true);
    }

    protected String generateToken(Map<String, Object> payload, Date issueTime, Date expireDate, boolean encrypted) {

        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet();

        Map<String, Object> customClaims = new HashMap<>();

        if (payload != null) {
            customClaims.putAll(payload);
        }

        if (expireDate != null) {
            claimsSet.setExpirationTime(expireDate);
        }

        claimsSet.setCustomClaims(customClaims);
        claimsSet.setSubject(SUBJECT.get());
        claimsSet.setIssueTime(issueTime);
        claimsSet.setIssuer(ISSUER.get());

        if (encrypted) {
            RSAPrivateKeyHolder privateKey = keyProvider.getPrivateKey();

            customClaims.put(KEY_ID, privateKey.getKeyId());
            // Create RSA-signer with the private key
            JWSSigner signer = new RSASSASigner(privateKey.getKey());

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            // Compute the RSA signature
            try {
                signedJWT.sign(signer);
            } catch (JOSEException e) {
                throw new RuntimeException("Failed to generate token", e);
            }

            return signedJWT.serialize();
        }
        JWEHeader header = new JWEHeader(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128GCM);
        EncryptedJWT jwt = new EncryptedJWT(header, claimsSet);
        RSAEncrypter encrypter = new RSAEncrypter((RSAPublicKey) keyProvider.getDefaultPublicKey());
        try {
            jwt.encrypt(encrypter);
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate encrypted token", e);
        }
        return jwt.serialize();

    }

    public RSAKeyProvider getKeyProvider() {
        return keyProvider;
    }

    @Override
    public boolean isValidToken(String token, boolean encrypted) throws JOSEException {
        if (encrypted) {
            return isValidEncryptedToken(token);
        }
        return isValidToken(token);
    }

    protected boolean isValidToken(String token) throws JOSEException {
        try {
            JWSObject jws = JWSObject.parse(token);
            return jws.verify(verifier);
        } catch (Exception e) {
            throw new JOSEException("Invalid Token", e);
        }
    }

    protected boolean isValidEncryptedToken(String token) throws JOSEException {
        try {
            EncryptedJWT jwt = EncryptedJWT.parse(token);
            jwt.decrypt(decrypter);
            return (JWEObject.State.DECRYPTED == jwt.getState());
        } catch (Exception e) {
            throw new JOSEException("Invalid Token", e);
        }
    }

    @Override
    public JSONObject getJsonPayload(String token, boolean encrypted) throws JOSEException {
        try {
            if (encrypted) {
                EncryptedJWT jwt = EncryptedJWT.parse(token);
                jwt.decrypt(decrypter);
                if (JWEObject.State.DECRYPTED != jwt.getState()) {
                    throw new JOSEException("ERROR: Could not decrypt token");
                }
                return jwt.getPayload().toJSONObject();
            }
            JWSObject jws = JWSObject.parse(token);
            if (!jws.verify(verifier)) {
                throw new JOSEException("ERROR: Fraudulent Signature in token");
            }
            return jws.getPayload().toJSONObject();
        } catch (Exception e) {
            throw new JOSEException("Invalid Token", e);
        }
    }

    @Inject
    public void setKeyProvider(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
        verifier = new RSASSAVerifier((RSAPublicKey) keyProvider.getDefaultPublicKey());
        decrypter = new RSADecrypter(keyProvider.getPrivateKey().getKey());
    }

}