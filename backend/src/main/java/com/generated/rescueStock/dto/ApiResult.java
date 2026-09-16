package com.generated.rescueStock.dto;

/** 统一成功响应包装（data 可为任意对象/列表）。 */
public class ApiResult<T> {
  public boolean success = true;
  public T data;

  public ApiResult() {}

  public ApiResult(T data) { this.data = data; }
}
