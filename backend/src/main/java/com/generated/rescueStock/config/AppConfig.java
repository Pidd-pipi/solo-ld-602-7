package com.generated.rescueStock.config;

import com.generated.rescueStock.middlewares.AuditLogMiddleware;
import com.generated.rescueStock.middlewares.AuthMiddleware;
import com.generated.rescueStock.middlewares.RateLimitMiddleware;
import com.generated.rescueStock.middlewares.RbacMiddleware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 中间件装配：限流 -> 认证 -> RBAC ->（审计在 afterCompletion 落库）。 */
@Configuration
public class AppConfig implements WebMvcConfigurer {
  private final RateLimitMiddleware rateLimit;
  private final AuthMiddleware auth;
  private final RbacMiddleware rbac;
  private final AuditLogMiddleware audit;

  public AppConfig(RateLimitMiddleware rateLimit, AuthMiddleware auth,
                   RbacMiddleware rbac, AuditLogMiddleware audit) {
    this.rateLimit = rateLimit;
    this.auth = auth;
    this.rbac = rbac;
    this.audit = audit;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(rateLimit).addPathPatterns("/api/**");
    registry.addInterceptor(auth).addPathPatterns("/api/**");
    registry.addInterceptor(rbac).addPathPatterns("/api/**");
    registry.addInterceptor(audit).addPathPatterns("/api/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}
