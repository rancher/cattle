package io.cattle.platform.register.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.netflix.config.DynamicLongProperty;

public class RegistrationToken {

    public static final String HMAC_SHA1_ALGORITHM  = "HmacSHA1";
    private static final DynamicLongProperty TOKEN_PERIOD = ArchaiusUtil.getLong("registration.token.period.millis");

    public static long getAllowedTime() {
        return TOKEN_PERIOD.get();
    }

    public static final String createToken(String accessKey, String secretKey) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return createToken(accessKey, secretKey, cal.getTime());
    }

    public static final String createToken(String accessKey, String secretKey, Date date) {
        String prefix = String.format("%s:%d", accessKey, date.getTime());

        try {
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            String signature = Base64.encodeBase64String(mac.doFinal(prefix.getBytes("UTF-8"))).replaceAll("[/=+]", "");

            return String.format("%s:%s", prefix, signature);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Failed to generate signature key for [" + prefix + "]", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate signature key for [" + prefix + "]", e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to generate signature key for [" + prefix + "]", e);
        }
    }
}
