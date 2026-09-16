package com.generated.rescueStock.dto;

/** 统一错误响应体。 */
public class ApiError {
  public boolean success = false;
  public String code;
  public String message;
  public String path;
  public long timestamp;

  public ApiError() {}

  public ApiError(String code, String message, String path, long timestamp) {
    this.code = code;
    this.message = message;
    this.path = path;
    this.timestamp = timestamp;
  }
}
