package com.generated.rescueStock.middlewares;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.constructors.ApiErrorFactory;
import com.generated.rescueStock.controllers.ApiException;
import com.generated.rescueStock.dto.ApiError;
import com.generated.rescueStock.services.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局错误处理中间件：service 的 BusinessException 与 controller 的 ApiException
 * 在此统一翻译成错误响应，但错误码/消息仍来自各自集中常量，不吞异常。
 */
@RestControllerAdvice
public class ErrorHandlerMiddleware {
  private static final Logger log = LoggerFactory.getLogger(ErrorHandlerMiddleware.class);

  /** 错误码 -> HTTP 状态。 */
  private HttpStatus httpStatusOf(String code) {
    return switch (code) {
      case ErrorCodes.AUTH_REQUIRED, ErrorCodes.AUTH_INVALID -> HttpStatus.UNAUTHORIZED;
      case ErrorCodes.RBAC_DENIED -> HttpStatus.FORBIDDEN;
      case ErrorCodes.ORDER_NOT_FOUND, ErrorCodes.BATCH_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCodes.ILLEGAL_STATUS_TRANSITION, ErrorCodes.ORDER_DUPLICATE_REQUEST,
           ErrorCodes.INSUFFICIENT_STOCK, ErrorCodes.BATCH_CONTENDED,
           ErrorCodes.CONCURRENT_CONFLICT, ErrorCodes.LEDGER_IMBALANCE,
           ErrorCodes.ALLOCATION_MISSING, ErrorCodes.WAREHOUSE_MISMATCH, ErrorCodes.LINE_EMPTY ->
          HttpStatus.CONFLICT;
      case ErrorCodes.VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
      case ErrorCodes.RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
    // 业务异常是预期内的拒绝（占库失败/状态非法），记录 info 而非 error
    log.info("business rejected code={} path={} args={}", ex.getCode(), req.getRequestURI(), ex.getArgs());
    ApiError body = ApiErrorFactory.ofCode(ex.getCode(), ex.getArgs(), req.getRequestURI());
    return ResponseEntity.status(httpStatusOf(ex.getCode())).body(body);
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
    ApiError body = new ApiError(ex.getCode(), ex.getMessage(), req.getRequestURI(), System.currentTimeMillis());
    return ResponseEntity.status(ex.getHttpStatus()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
    String reason = ex.getBindingResult().getFieldErrors().stream()
        .findFirst().map(f -> f.getField() + " " + f.getDefaultMessage()).orElse("字段不合法");
    ApiError body = ApiErrorFactory.ofCode(
        ErrorCodes.VALIDATION_FAILED, Map.of("reason", reason), req.getRequestURI());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
    log.error("unhandled error path={}", req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorFactory.internal(req.getRequestURI()));
  }
}
