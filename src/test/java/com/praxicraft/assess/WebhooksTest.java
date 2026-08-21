package com.praxicraft.assess;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhooksTest {
  @Test
  void verifySignaturePrefixed() throws Exception {
    String secret = "whsec_test";
    String body = "{\"ok\":true}";
    String digest = hmac(secret, body);
    assertTrue(Webhooks.verifySignature(secret, body, "sha256=" + digest));
  }

  @Test
  void verifySignatureLegacyHex() throws Exception {
    String secret = "whsec_test";
    String body = "{\"ok\":true}";
    String digest = hmac(secret, body);
    assertTrue(Webhooks.verifySignature(secret, body, digest));
  }

  @Test
  void verifySignatureRejectsBad() {
    assertFalse(Webhooks.verifySignature("whsec_test", "{}", "sha256=deadbeef"));
  }

  @Test
  void nullBodyIsEmpty() throws Exception {
    String secret = "whsec_test";
    String digest = hmac(secret, "");
    assertTrue(Webhooks.verifySignature(secret, (String) null, "sha256=" + digest));
  }

  @Test
  void verifySignatureBytes() throws Exception {
    String secret = "whsec_test";
    byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
    String digest = hmac(secret, new String(body, StandardCharsets.UTF_8));
    assertTrue(Webhooks.verifySignature(secret, body, "sha256=" + digest));
  }

  private static String hmac(String secret, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }
}
