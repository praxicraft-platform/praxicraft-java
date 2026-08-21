package com.praxicraft.assess.exception;

/** Base exception for all Praxicraft SDK errors. */
public class PraxicraftException extends RuntimeException {
  private final String errorCode;

  public PraxicraftException(String message) {
    this(message, null, null);
  }

  public PraxicraftException(String message, String errorCode) {
    this(message, errorCode, null);
  }

  public PraxicraftException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
