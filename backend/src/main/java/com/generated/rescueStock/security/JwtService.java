package com.generated.rescueStock.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 极简自包含 JWT（HS256），不引入第三方库。
 * payload 仅放本地账号 id/username/role，符合“全部本地、禁止第三方 API”。
 */
@Component
public class JwtService {
  private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder B64D = Base64.getUrlDecoder();
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${rescue.jwt.secret}")
  private String secret;

  @Value("${rescue.jwt.expire-seconds:43200}")
  private long expireSeconds;

  public String issue(Long userId, String username, String displayName, String role) {
    try {
      String header = B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
      long now = System.currentTimeMillis();
      var payload = mapper.createObjectNode()
          .put("sub", userId)
          .put("username", username)
          .put("displayName", displayName)
          .put("role", role)
          .put("iat", now / 1000)
          .put("exp", (now + expireSeconds * 1000) / 1000);
      String body = header + "." + B64.encodeToString(mapper.writeValueAsBytes(payload));
      return body + "." + sign(body);
    } catch (Exception e) {
      throw new IllegalStateException("failed to issue token", e);
    }
  }

  public CurrentUser verify(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) return null;
      String body = parts[0] + "." + parts[1];
      if (!constantEquals(sign(body), parts[2])) return null;
      var node = mapper.readTree(B64D.decode(parts[1]));
      long exp = node.path("exp").asLong();
      if (exp < System.currentTimeMillis() / 1000) return null;
      return new CurrentUser(
          node.path("sub").asLong(),
          node.path("username").asText(),
          node.path("displayName").asText(),
          node.path("role").asText());
    } catch (Exception e) {
      return null;
    }
  }

  private String sign(String content) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return B64.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
  }

  private boolean constantEquals(String a, String b) {
    if (a.length() != b.length()) return false;
    int diff = 0;
    for (int i = 0; i < a.length(); i++) {
      diff |= a.charAt(i) ^ b.charAt(i);
    }
    return diff == 0;
  }
}
