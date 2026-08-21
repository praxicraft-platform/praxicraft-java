package com.praxicraft.assess.exception;

/** Network / transport failure talking to the Praxicraft API. */
public class ApiConnectionException extends ApiException {
  public ApiConnectionException() {
    this("Failed to connect to the Praxicraft API.");
  }

  public ApiConnectionException(String message) {
    super(message, "CONNECTION_ERROR");
  }

  public ApiConnectionException(String message, Throwable cause) {
    super(message, "CONNECTION_ERROR", cause);
  }
}
