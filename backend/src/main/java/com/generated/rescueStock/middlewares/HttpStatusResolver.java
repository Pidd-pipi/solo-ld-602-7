package com.generated.rescueStock.middlewares;

import com.generated.rescueStock.constants.ErrorCodes;
import org.springframework.http.HttpStatus;

/** 错误码到 HTTP 状态的统一映射，全局处理器与 controller 包装共用。 */
public final class HttpStatusResolver {
  public static HttpStatus resolve(String code) {
    return switch (code) {
      case ErrorCodes.AUTH_REQUIRED, ErrorCodes.AUTH_INVALID -> HttpStatus.UNAUTHORIZED;
      case ErrorCodes.RBAC_DENIED -> HttpStatus.FORBIDDEN;
      case ErrorCodes.ORDER_NOT_FOUND, ErrorCodes.BATCH_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCodes.VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      case ErrorCodes.RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      default -> HttpStatus.CONFLICT; // 库存争抢/状态非法/幂等等业务冲突
    };
  }

  private HttpStatusResolver() {}
}
