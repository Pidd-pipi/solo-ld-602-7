package com.generated.rescueStock.controllers;

import com.generated.rescueStock.constants.ErrorMessages;
import com.generated.rescueStock.middlewares.HttpStatusResolver;
import com.generated.rescueStock.services.BusinessException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * controller 层异常包装：service 抛 BusinessException，controller 在此二次包装为 ApiException，
 * 体现 service/controller 各自包装，而不是只在全局处理器一处吞掉全部异常。
 */
public final class ControllerSupport {

  public static ApiException wrap(BusinessException ex) {
    return new ApiException(
        HttpStatusResolver.resolve(ex.getCode()).value(),
        ex.getCode(),
        ErrorMessages.render(ex.getCode(), ex.getArgs()),
        ex);
  }

  public static <T> T call(Supplier<T> supplier) {
    try {
      return supplier.get();
    } catch (BusinessException ex) {
      throw wrap(ex);
    }
  }

  public static void run(Runnable runnable) {
    try {
      runnable.run();
    } catch (BusinessException ex) {
      throw wrap(ex);
    }
  }

  public static Map<String, Object> created(boolean idempotent, Long id) {
    return Map.of("id", id, "idempotent", idempotent);
  }

  private ControllerSupport() {}
}
