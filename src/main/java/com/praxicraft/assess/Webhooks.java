package com.praxicraft.assess;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Helpers for verifying inbound webhook signatures. */
public final class Webhooks {
  private Webhooks() {}

  /**
   * Verify an {@code X-Praxicraft-Signature} header.
   *
   * @param secret webhook signing secret
   * @param body raw request body (null treated as empty)
   * @param headerSig value of the signature header
   */
  public static boolean verifySignature(String secret, String body, String headerSig) {
    if (secret == null || secret.isEmpty() || headerSig == null || headerSig.isEmpty()) {
      return false;
    }
    byte[] payload = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
    return verifySignature(secret, payload, headerSig);
  }

  /**
   * Verify an {@code X-Praxicraft-Signature} header against raw body bytes.
   */
  public static boolean verifySignature(String secret, byte[] body, String headerSig) {
    if (secret == null || secret.isEmpty() || headerSig == null || headerSig.isEmpty()) {
      return false;
    }
    byte[] payload = body == null ? new byte[0] : body;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String digest = HexFormat.of().formatHex(mac.doFinal(payload));
      String expected = "sha256=" + digest;
      if (headerSig.startsWith("sha256=")) {
        return constantTimeEquals(expected, headerSig);
      }
      return constantTimeEquals(digest, headerSig) || constantTimeEquals(expected, headerSig);
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) {
      return false;
    }
    byte[] aa = a.getBytes(StandardCharsets.UTF_8);
    byte[] bb = b.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(aa, bb);
  }
}
