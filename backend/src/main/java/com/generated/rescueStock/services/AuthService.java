package com.generated.rescueStock.services;

import com.generated.rescueStock.constants.ErrorCodes;
import com.generated.rescueStock.repositories.AppUserRepository;
import com.generated.rescueStock.repositories.AuditLogRepository;
import com.generated.rescueStock.security.JwtService;
import com.generated.rescueStock.types.LoginPayload;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 本地账号登录，签发 JWT；不接任何第三方。 */
@Service
public class AuthService {
  private final AppUserRepository userRepo;
  private final AuditLogRepository auditRepo;
  private final JwtService jwt;

  public AuthService(AppUserRepository userRepo, AuditLogRepository auditRepo, JwtService jwt) {
    this.userRepo = userRepo;
    this.auditRepo = auditRepo;
    this.jwt = jwt;
  }

  public Map<String, Object> login(LoginPayload payload) {
    Map<String, Object> user = userRepo.findByUsername(payload.username);
    String hash = sha256(payload.password == null ? "" : payload.password);
    if (user == null || !hash.equals(String.valueOf(user.get("password_hash")))) {
      auditRepo.append(payload.username, "", "LOGIN", "Auth", payload.username, "登录失败", "FAILED");
      throw new BusinessException(ErrorCodes.AUTH_INVALID, Map.of());
    }
    String token = jwt.issue(
        ((Number) user.get("id")).longValue(),
        String.valueOf(user.get("username")),
        String.valueOf(user.get("display_name")),
        String.valueOf(user.get("role")));
    auditRepo.append(String.valueOf(user.get("username")), String.valueOf(user.get("role")),
        "LOGIN", "Auth", String.valueOf(user.get("id")), "登录成功", "SUCCESS");
    return Map.of(
        "token", token,
        "user", Map.of(
            "id", user.get("id"),
            "username", user.get("username"),
            "displayName", user.get("display_name"),
            "role", user.get("role")));
  }

  static String sha256(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
