package com.generated.rescueStock.controllers;

/** controller 层包装异常：确保 service 抛出的异常不会在控制层被静默吞掉。 */
public class ApiException extends RuntimeException {
  private final int httpStatus;
  private final String code;

  public ApiException(int httpStatus, String code, String message, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.code = code;
  }

  public int getHttpStatus() { return httpStatus; }
  public String getCode() { return code; }
}
