package io.cattle.platform.token.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.token.TokenService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

public class JwtTokenServiceImpl implements TokenService {

    private static final String KEY_ID = "kid";

    private static final DynamicStringProperty SUBJECT = ArchaiusUtil.getString("jwt.default.subject");
    private static final DynamicStringProperty ISSUER = ArchaiusUtil.getString("jwt.default.issuer");

    RSAKeyProvider keyProvider;

    @Override
    public String generateToken(Map<String, Object> payload) {
        return generateToken(payload, new Date());
    }

    protected String generateToken(Map<String, Object> payload, Date issueTime) {
        RSAPrivateKeyHolder privateKey = keyProvider.getPrivateKey();

        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(privateKey.getKey());

        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet();

        Map<String,Object> customClaims = new HashMap<>();
        customClaims.put(KEY_ID, privateKey.getKeyId());

        if ( payload != null ) {
            customClaims.putAll(payload);
        }

        claimsSet.setCustomClaims(customClaims);
        claimsSet.setSubject(SUBJECT.get());
        claimsSet.setIssueTime(issueTime);
        claimsSet.setIssuer(ISSUER.get());

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

        // Compute the RSA signature
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate token", e);
        }

        return signedJWT.serialize();
    }

    public RSAKeyProvider getKeyProvider() {
        return keyProvider;
    }

    @Inject
    public void setKeyProvider(RSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

}