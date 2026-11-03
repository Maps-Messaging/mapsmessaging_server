package io.mapsmessaging.rest.api.pagination;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CursorCodec {
  private static final String ALGORITHM = "HmacSHA256";
  private static final long LIFETIME_MILLIS = 30 * 60 * 1000L;
  private final byte[] key;
  private final Clock clock;

  public CursorCodec() {
    this(randomKey(), Clock.systemUTC());
  }

  CursorCodec(byte[] key, Clock clock) {
    this.key = key.clone();
    this.clock = clock;
  }

  private static byte[] randomKey() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  public String encode(String lastKey, String scope) {
    String payload = "1\n" + (clock.millis() + LIFETIME_MILLIS) + "\n"
        + encodeText(scope) + "\n" + encodeText(lastKey);
    String body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body));
  }

  public String decode(String cursor, String scope) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String[] components = cursor.split("\\.", -1);
      if (components.length != 2 || !MessageDigest.isEqual(sign(components[0]), Base64.getUrlDecoder().decode(components[1]))) {
        throw new IllegalArgumentException("Invalid pagination cursor");
      }
      String[] fields = new String(Base64.getUrlDecoder().decode(components[0]), StandardCharsets.UTF_8).split("\\n", -1);
      if (fields.length != 4 || !"1".equals(fields[0]) || !scope.equals(decodeText(fields[2]))) {
        throw new IllegalArgumentException("Cursor does not match this request");
      }
      if (Long.parseLong(fields[1]) < clock.millis()) {
        throw new IllegalArgumentException("Pagination cursor has expired");
      }
      return decodeText(fields[3]);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid or expired pagination cursor: " + exception.getMessage(), exception);
    }
  }

  private static String encodeText(String text) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String decodeText(String text) {
    return new String(Base64.getUrlDecoder().decode(text), StandardCharsets.UTF_8);
  }

  private byte[] sign(String body) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("Unable to sign pagination cursor", exception);
    }
  }
}
