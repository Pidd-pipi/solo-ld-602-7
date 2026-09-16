package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;
import java.util.Map;

/** service 层业务异常：携带集中错误码与渲染参数，controller/全局处理不得吞掉。 */
public class BusinessException extends RuntimeException {
  private final String code;
  private final transient Map<String, Object> args;

  public BusinessException(String code) {
    this(code, Map.of(), null);
  }

  public BusinessException(String code, Map<String, Object> args) {
    this(code, args, null);
  }

  public BusinessException(String code, Map<String, Object> args, Throwable cause) {
    super(code, cause);
    this.code = code;
    this.args = args;
  }

  public String getCode() { return code; }
  public Map<String, Object> getArgs() { return args; }

  public static BusinessException of(String code, Map<String, Object> args) {
    return new BusinessException(code, args);
  }

  public static BusinessException conflict(Throwable cause) {
    return new BusinessException(ErrorCodes.CONCURRENT_CONFLICT, Map.of(), cause);
  }
}
