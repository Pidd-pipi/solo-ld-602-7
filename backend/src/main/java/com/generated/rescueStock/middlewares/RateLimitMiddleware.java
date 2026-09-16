package com.generated.rescueStock.middlewares;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 简易限流中间件：每 IP 每分钟固定窗口（默认 120 次），全本地实现。
 * 防止连点重复提交把占库/回补打爆。
 */
@Component
public class RateLimitMiddleware implements HandlerInterceptor {
  private static final long WINDOW_MS = 60_000L;
  private static final int MAX_HITS = 120;

  private record Window(long start, int hits) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
    if (AuthMiddleware.isPublicPath(req.getRequestURI()) && !req.getRequestURI().contains("/login")) {
      return true;
    }
    String key = req.getRemoteAddr();
    long now = System.currentTimeMillis();
    synchronized (windows) {
      Window w = windows.get(key);
      if (w == null || now - w.start() > WINDOW_MS) {
        w = new Window(now, 0);
      }
      int hits = w.hits() + 1;
      windows.put(key, new Window(w.start(), hits));
      if (hits > MAX_HITS) {
        resp.setStatus(429);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"success\":false,\"code\":\"RATE_LIMITED\","
            + "\"message\":\"操作过于频繁，请稍后再试\"}");
        return false;
      }
    }
    return true;
  }
}
