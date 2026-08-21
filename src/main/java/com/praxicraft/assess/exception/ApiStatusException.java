package com.praxicraft.assess.exception;

import java.util.Collections;
import java.util.Map;

/** Non-2xx HTTP response from the API. */
public class ApiStatusException extends ApiException {
  private final int statusCode;
  private final Object details;
  private final Object responseBody;
  private final Map<String, String> headers;
  private final String requiredPlan;
  private final Double retryAfter;

  public ApiStatusException(
      String message,
      int statusCode,
      String errorCode,
      Object details,
      Object responseBody,
      Map<String, String> headers,
      String requiredPlan,
      Double retryAfter) {
    super(message, errorCode);
    this.statusCode = statusCode;
    this.details = details;
    this.responseBody = responseBody;
    this.headers =
        headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    this.requiredPlan = requiredPlan;
    this.retryAfter = retryAfter;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Object getDetails() {
    return details;
  }

  public Object getResponseBody() {
    return responseBody;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getRequiredPlan() {
    return requiredPlan;
  }

  public Double getRetryAfter() {
    return retryAfter;
  }
}
