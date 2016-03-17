package io.cattle.platform.token.impl;

import static org.junit.Assert.*;

import io.cattle.platform.token.CertSet;
import io.cattle.platform.token.TokenException;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import com.nimbusds.jose.util.Base64;

public class JwtTokenServiceImplTest {

    private static final String KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJ6/qHmPd2DhNFRL/F+t6vi8PLlNL+Pa8EI7r9a0ct4JKKgc7VVYb4Y3BlC1bfwmoUA3ISnZOZrH29G408E0Yy17PkBReWTTirSUatfYK1aEiRVQpdbxtAVtCp3QGL6t4x5c/RDcOXlkuM1pK8czNWxS5rVsFtFLttoH48/2xgVrAgMBAAECgYB8xXrvgFl95cOxdb+4nAOQ2LKJmidH2a74/9ymzlFyPpSb/ZB0gfu1682k5dS6WMpopGwGblueUfNuFO0qb7h6qba+yoM0wQtbZ4sRLRJSEhitto2iwRdoloAs6193hZpsWr+fr/ePvd7mH/ESYTLUpx5s4jgSg5+m4fB3nWuHUQJBAOM1ibIF9IrXr65MpGJK5kvyFshiJw7w+YStSCV4HvD+hbbZkfHt0qHl0Akam3kPXxpnBETtDzdkgHELFSWr8I8CQQCy3VLUrb1BtgdW8UeHO/BLJMvxQw3psGu/ctFFODEaNU7M3UJDtTlBwyzu4F1dgOb+DqgI84XYeSR+Jj4fUpNlAkA2JjVJ0zeDu4GoFaX7swQNx4V8fj/2xKGC6FVQcL9XCiHOAS5SLS7M3NtmwAubn3k37qNK1gCRYAdoaY7BqqerAkEAmYNLbC4RmSxZ8Ez1TyQqgNP8Ff2vGzrHv3EUG4y21/+YukvMa8BGfCK/leYLPA0+NB7wKX6ZCcovf/Xiq4974QJAUJo0snUbHz71UHTFunrJ/VAJZ536Igcb7ICIgUKKUX8rOJo9luQ5Ok5GwlT9bkwigOYN45rylf0Znn46CfT1zQ==";
    private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE0MTM5MzY2MjYsImtpZCI6ImFiYyJ9.m_HWELiAzOxfZ36yajxfp-EBzrkyRy8-Ahn1zYxufqjYRllhKXd2_eOYSYh5fBA2dSgy6udZeKZm6nSQyA9dWLDq3OfaRcqHhFwBFGy3cGJ_VLr_eqBzZdJ3yjN3TLInGkTM9G1K7j4VnnCUuUdwXOV37UabGnxHEb7ZtQok3UQ";

    JwtTokenServiceImpl impl;

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
        impl = new JwtTokenServiceImpl();
        impl.setKeyProvider(new RSAKeyProvider() {

            KeyFactory kf;
            EncodedKeySpec spec = new PKCS8EncodedKeySpec(new Base64(KEY).decode());

            @Override
            public RSAPrivateKeyHolder getPrivateKey() {

                try {
                    kf = KeyFactory.getInstance("RSA");
                    return new RSAPrivateKeyHolder("abc", (RSAPrivateKey) kf.generatePrivate(spec));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Map<String, PublicKey> getPublicKeys() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public PublicKey getDefaultPublicKey() {
                try {
                    kf = KeyFactory.getInstance("RSA");
                    BigInteger modulus = new BigInteger(
                            "111477103238322465633334802347196848276745427190035850232359047430738831490294428792865542779043266665451160648116725279287065632589519313377918207473210865843357067938152969267052295101676828476867765239574399207781254529735105609482031252978262212237371891597488765482508817144842927535892383110624969098603");
                    BigInteger exponent = new BigInteger("65537");
                    RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(modulus, exponent);
                    return kf.generatePublic(publicKeySpec);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public CertSet generateCertificate(String subject) throws Exception {
                return null;
            }
        });

    }

    @Test
    public void testGeneratesToken() {
        assertEquals(TOKEN, impl.generateToken(null, new Date(1413936626719L), null, false));
    }

    @Test(expected = TokenException.class)
    public void testCheckExpiry() throws TokenException {
        String expiredToken = impl.generateToken(null, new Date(1413936626719L), new Date(1413936626719L), false);
        impl.getJsonPayload(expiredToken, false);
    }

    @Test
    public void testDecryptsEncryptedToken() throws TokenException {
        String newEncryptedToken = impl.generateToken(null, new Date(1413936626719L), new Date(1923109200000L), true);
        Map<String, Object> decrypted = impl.getJsonPayload(newEncryptedToken, true);
        assertEquals(decrypted.get("exp"), Long.valueOf(1923109200));
        assertEquals(decrypted.get("iat"), Long.valueOf(1413936626));
    }
}
