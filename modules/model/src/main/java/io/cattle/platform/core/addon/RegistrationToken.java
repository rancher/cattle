package io.cattle.platform.core.addon;

import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.github.ibuildthecloud.gdapi.annotation.Type;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Type(list = false)
public class RegistrationToken {

    public static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final DynamicLongProperty TOKEN_PERIOD = ArchaiusUtil.getLong("registration.token.period.millis");

    String hostCommand, clusterCommand, image, token;

    public RegistrationToken() {
    }

    public RegistrationToken(Credential cred) {
        this.token = createToken(cred.getPublicValue(), cred.getSecretValue());
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHostCommand() {
        return hostCommand;
    }

    public void setHostCommand(String hostCommand) {
        this.hostCommand = hostCommand;
    }

    public String getClusterCommand() {
        return clusterCommand;
    }

    public void setClusterCommand(String clusterCommand) {
        this.clusterCommand = clusterCommand;
    }

    public static long getAllowedTime() {
        return TOKEN_PERIOD.get();
    }

    public static final String createToken(String accessKey, String secretKey) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_YEAR, 0);

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
        } catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to generate signature key for [" + prefix + "]", e);
        }
    }
}
