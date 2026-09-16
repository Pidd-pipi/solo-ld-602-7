package com.generated.rescueStock.middlewares;

import com.generated.rescueStock.repositories.AuditLogRepository;
import com.generated.rescueStock.security.CurrentUser;
import com.generated.rescueStock.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求级审计中间件：对每个非 GET 的 /api 写请求落一条审计记录，
 * 成功与否看响应状态。业务级明细由 service 另写，二者互补。
 */
@Component
public class AuditLogMiddleware implements HandlerInterceptor {
  private final AuditLogRepository auditRepo;

  public AuditLogMiddleware(AuditLogRepository auditRepo) { this.auditRepo = auditRepo; }

  @Override
  public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
    String path = req.getRequestURI();
    String method = req.getMethod();
    if ("GET".equalsIgnoreCase(method) || !path.startsWith("/api")
        || AuthMiddleware.isPublicPath(path)) {
      return;
    }
    CurrentUser user = UserContextHolder.get();
    String actor = user == null ? "anonymous" : user.username;
    String role = user == null ? "" : user.role;
    String result = resp.getStatus() < 400 ? "SUCCESS" : "FAILED";
    try {
      auditRepo.append(actor, role, "API:" + method, "HTTP_REQUEST", path,
          method + " " + path + " -> " + resp.getStatus(), result);
    } catch (Exception ignored) {
      // 审计自身失败不能影响主流程结果
    }
  }
}
