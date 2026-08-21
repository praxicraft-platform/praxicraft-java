package com.praxicraft.assess.exception;

/** General API / client usage error (missing key, invalid args, etc.). */
public class ApiException extends PraxicraftException {
  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, String errorCode) {
    super(message, errorCode);
  }

  public ApiException(String message, String errorCode, Throwable cause) {
    super(message, errorCode, cause);
  }
}
