package com.praxicraft.assess.exception;

import java.util.Map;

/** Mapped HTTP error from the Assess Public API. */
public class RateLimitException extends ApiStatusException {
  public RateLimitException(
      String message,
      int statusCode,
      String errorCode,
      Object details,
      Object responseBody,
      Map<String, String> headers,
      String requiredPlan,
      Double retryAfter) {
    super(message, statusCode, errorCode, details, responseBody, headers, requiredPlan, retryAfter);
  }
}
