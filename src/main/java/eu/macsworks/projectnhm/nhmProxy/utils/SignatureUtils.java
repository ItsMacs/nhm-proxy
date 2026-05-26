package eu.macsworks.projectnhm.games.nhmGames.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

@UtilityClass
public class SignatureUtils {

    private final HmacUtils HMAC = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            Objects.requireNonNull(System.getenv("NHM_SECRET"), "NHM_SECRET env var must be set"));

    public String sign(String data) {
        return HMAC.hmacHex(data);
    }

    public boolean isSignatureValid(String data, String signature) {
        if (signature == null) return false;
        return MessageDigest.isEqual(
                sign(data).getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII)
        );
    }

}
