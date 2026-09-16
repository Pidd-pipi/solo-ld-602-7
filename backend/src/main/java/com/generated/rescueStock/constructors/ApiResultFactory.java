package com.generated.rescueStock.constructors;

import com.generated.rescueStock.dto.ApiResult;

/** 成功响应构造器。 */
public final class ApiResultFactory {
  public static <T> ApiResult<T> ok(T data) { return new ApiResult<>(data); }
  private ApiResultFactory() {}
}
