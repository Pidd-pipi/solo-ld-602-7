package com.generated.rescueStock.middlewares;

import com.generated.rescueStock.security.CurrentUser;
import com.generated.rescueStock.security.JwtService;
import com.generated.rescueStock.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 认证中间件：校验 JWT 并写入线程上下文；登录与健康检查放行。 */
@Component
public class AuthMiddleware implements HandlerInterceptor {
  private final JwtService jwt;

  public AuthMiddleware(JwtService jwt) { this.jwt = jwt; }

  public static boolean isPublicPath(String path) {
    return "/api/auth/login".equals(path) || "/health".equals(path)
        || path.startsWith("/actuator");
  }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
    String path = req.getRequestURI();
    if ("OPTIONS".equalsIgnoreCase(req.getMethod()) || isPublicPath(path)) {
      return true;
    }
    String header = req.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      writeUnauthorized(resp, "AUTH_REQUIRED", "请先登录后再执行该操作");
      return false;
    }
    CurrentUser user = jwt.verify(header.substring(7));
    if (user == null) {
      writeUnauthorized(resp, "AUTH_INVALID", "登录凭据无效或已过期");
      return false;
    }
    UserContextHolder.set(user);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
    UserContextHolder.clear();
  }

  static void writeUnauthorized(HttpServletResponse resp, String code, String message) throws Exception {
    resp.setStatus(401);
    resp.setContentType("application/json;charset=UTF-8");
    resp.getWriter().write("{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
  }
}
