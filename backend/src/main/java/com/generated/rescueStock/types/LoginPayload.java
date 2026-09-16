package com.generated.rescueStock.types;

import jakarta.validation.constraints.NotBlank;

/** 登录请求。 */
public class LoginPayload {
  @NotBlank public String username;
  @NotBlank public String password;
}
