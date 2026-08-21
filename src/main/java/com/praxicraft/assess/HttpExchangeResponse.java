package com.praxicraft.assess;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Result of an HTTP exchange returned by {@link HttpHandler}. */
public final class HttpExchangeResponse {
  private final int status;
  private final Map<String, String> headers;
  private final String body;

  public HttpExchangeResponse(int status, Map<String, String> headers, String body) {
    this.status = status;
    Map<String, String> copy = new LinkedHashMap<>();
    if (headers != null) {
      for (Map.Entry<String, String> e : headers.entrySet()) {
        if (e.getKey() != null) {
          copy.put(e.getKey().toLowerCase(), e.getValue());
        }
      }
    }
    this.headers = Collections.unmodifiableMap(copy);
    this.body = body == null ? "" : body;
  }

  public int status() {
    return status;
  }

  public Map<String, String> headers() {
    return headers;
  }

  public String body() {
    return body;
  }

  @Override
  public String toString() {
    return "HttpExchangeResponse{status=" + status + ", bodyLength=" + body.length() + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HttpExchangeResponse that)) return false;
    return status == that.status
        && Objects.equals(headers, that.headers)
        && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, headers, body);
  }
}
