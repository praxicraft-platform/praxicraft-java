package com.praxicraft.assess;

import java.util.Map;

/**
 * Injectable HTTP transport for tests and custom clients.
 *
 * <p>Implementations must return status, response headers (lower-cased keys preferred), and raw body.
 */
@FunctionalInterface
public interface HttpHandler {
  HttpExchangeResponse handle(String method, String url, Map<String, String> headers, String body)
      throws Exception;
}
