package com.generated.rescueStock.controllers;

import com.generated.rescueStock.routes.AuthRoutes;
import com.generated.rescueStock.services.AuthService;
import com.generated.rescueStock.types.LoginPayload;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** 本地账号登录，返回 JWT。 */
@RestController
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) { this.authService = authService; }

  @PostMapping(AuthRoutes.LOGIN)
  public Map<String, Object> login(@Valid @RequestBody LoginPayload payload) {
    return ControllerSupport.call(() -> authService.login(payload));
  }
}
