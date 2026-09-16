package com.generated.rescueStock.middlewares;

import com.generated.rescueStock.constants.UserRole;
import com.generated.rescueStock.security.CurrentUser;
import com.generated.rescueStock.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RBAC 中间件：按 方法 + 路径动作 控制角色。
 * GET 对所有登录角色开放（含 OBSERVER）；写操作按下表限制。
 */
@Component
public class RbacMiddleware implements HandlerInterceptor {

  private static final Set<String> ALL = Set.of(
      UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER, UserRole.APPROVER, UserRole.OBSERVER);

  // 动作关键词 -> 允许角色
  private static final Map<String, Set<String>> ACTION_ROLES = Map.of(
      "approve", Set.of(UserRole.STREET_ADMIN, UserRole.APPROVER),
      "reject", Set.of(UserRole.STREET_ADMIN, UserRole.APPROVER),
      "outbound", Set.of(UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER),
      "receive", Set.of(UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER),
      "refuse", Set.of(UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER),
      "cancel", Set.of(UserRole.STREET_ADMIN)
  );

  // 资源写操作 -> 允许角色
  private static final Map<String, Set<String>> RESOURCE_ROLES = Map.of(
      "batches", Set.of(UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER),
      "shelters", Set.of(UserRole.STREET_ADMIN),
      "events", Set.of(UserRole.STREET_ADMIN, UserRole.APPROVER)
  );

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
    String path = req.getRequestURI();
    String method = req.getMethod();
    if ("OPTIONS".equalsIgnoreCase(method) || AuthMiddleware.isPublicPath(path) || "GET".equalsIgnoreCase(method)) {
      return true;
    }
    CurrentUser user = UserContextHolder.get();
    if (user == null) {
      AuthMiddleware.writeUnauthorized(resp, "AUTH_REQUIRED", "请先登录后再执行该操作");
      return false;
    }

    String deniedAction = matchDeniedAction(path, user.role);
    if (deniedAction != null) {
      resp.setStatus(403);
      resp.setContentType("application/json;charset=UTF-8");
      resp.getWriter().write("{\"success\":false,\"code\":\"RBAC_DENIED\","
          + "\"message\":\"当前角色 " + user.role + " 无权执行 " + deniedAction + "\"}");
      return false;
    }
    return true;
  }

  /** 返回被拒绝的动作名；允许时返回 null。 */
  private String matchDeniedAction(String path, String role) {
    // 调拨动作：/api/dispatch-orders/{id}/{approve|reject|outbound|receive|refuse|cancel}
    for (Map.Entry<String, Set<String>> e : ACTION_ROLES.entrySet()) {
      if (path.endsWith("/" + e.getKey()) && path.contains("/dispatch-orders/")) {
        return e.getValue().contains(role) ? null : e.getKey();
      }
    }
    // 创建调拨申请
    if (path.equals("/api/dispatch-orders")) {
      return Set.of(UserRole.STREET_ADMIN, UserRole.WAREHOUSE_KEEPER).contains(role) ? null : "apply";
    }
    // 其它资源写操作
    for (Map.Entry<String, Set<String>> e : RESOURCE_ROLES.entrySet()) {
      if (path.startsWith("/api/" + e.getKey())) {
        return e.getValue().contains(role) ? null : e.getKey();
      }
    }
    // 未显式配置的写操作：拒绝只读观察员
    if (UserRole.OBSERVER.equals(role)) {
      return "write";
    }
    return null;
  }
}
