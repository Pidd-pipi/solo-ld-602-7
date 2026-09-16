package com.generated.rescueStock.constructors;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constants.ErrorMessages;
import com.generated.rescueStock.dto.ApiError;
import java.util.Map;

/** 统一错误响应构造器，controller 与全局异常处理都只能通过这里构造错误体。 */
public final class ApiErrorFactory {

  public static ApiError ofCode(String code, Map<String, Object> args, String path) {
    return new ApiError(code, ErrorMessages.render(code, args), path, System.currentTimeMillis());
  }

  public static ApiError internal(String path) {
    return new ApiError(ErrorCodes.INTERNAL_ERROR, ErrorMessages.of(ErrorCodes.INTERNAL_ERROR),
        path, System.currentTimeMillis());
  }

  private ApiErrorFactory() {}
}
