package io.cattle.platform.token.impl;

import static org.junit.Assert.*;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.nimbusds.jose.util.Base64;

public class JwtTokenServiceImplTest {

    private static final String KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJ6/qHmPd2DhNFRL/F+t6vi8PLlNL+Pa8EI7r9a0ct4JKKgc7VVYb4Y3BlC1bfwmoUA3ISnZOZrH29G408E0Yy17PkBReWTTirSUatfYK1aEiRVQpdbxtAVtCp3QGL6t4x5c/RDcOXlkuM1pK8czNWxS5rVsFtFLttoH48/2xgVrAgMBAAECgYB8xXrvgFl95cOxdb+4nAOQ2LKJmidH2a74/9ymzlFyPpSb/ZB0gfu1682k5dS6WMpopGwGblueUfNuFO0qb7h6qba+yoM0wQtbZ4sRLRJSEhitto2iwRdoloAs6193hZpsWr+fr/ePvd7mH/ESYTLUpx5s4jgSg5+m4fB3nWuHUQJBAOM1ibIF9IrXr65MpGJK5kvyFshiJw7w+YStSCV4HvD+hbbZkfHt0qHl0Akam3kPXxpnBETtDzdkgHELFSWr8I8CQQCy3VLUrb1BtgdW8UeHO/BLJMvxQw3psGu/ctFFODEaNU7M3UJDtTlBwyzu4F1dgOb+DqgI84XYeSR+Jj4fUpNlAkA2JjVJ0zeDu4GoFaX7swQNx4V8fj/2xKGC6FVQcL9XCiHOAS5SLS7M3NtmwAubn3k37qNK1gCRYAdoaY7BqqerAkEAmYNLbC4RmSxZ8Ez1TyQqgNP8Ff2vGzrHv3EUG4y21/+YukvMa8BGfCK/leYLPA0+NB7wKX6ZCcovf/Xiq4974QJAUJo0snUbHz71UHTFunrJ/VAJZ536Igcb7ICIgUKKUX8rOJo9luQ5Ok5GwlT9bkwigOYN45rylf0Znn46CfT1zQ==";
    private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE0MTM5MzY2MjYsImtpZCI6ImFiYyJ9.m_HWELiAzOxfZ36yajxfp-EBzrkyRy8-Ahn1zYxufqjYRllhKXd2_eOYSYh5fBA2dSgy6udZeKZm6nSQyA9dWLDq3OfaRcqHhFwBFGy3cGJ_VLr_eqBzZdJ3yjN3TLInGkTM9G1K7j4VnnCUuUdwXOV37UabGnxHEb7ZtQok3UQ";

    JwtTokenServiceImpl impl;

    @Before
    public void setUp() {
        impl = new JwtTokenServiceImpl();
        impl.setKeyProvider(new RSAKeyProvider() {
            @Override
            public RSAPrivateKeyHolder getPrivateKey() {

                try {
                    EncodedKeySpec spec = new PKCS8EncodedKeySpec(new Base64(KEY).decode());
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    return new RSAPrivateKeyHolder("abc", (RSAPrivateKey)kf.generatePrivate(spec));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeySpecException e) {
                    throw new RuntimeException(e);
                }
            }
        });


    }

    @Test
    public void test() {
        assertEquals(TOKEN, impl.generateToken(null, new Date(1413936626719L)));
    }

}
