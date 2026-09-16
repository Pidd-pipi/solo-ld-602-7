package com.generated.rescueStock.security;

/** 当前登录用户（由 AuthMiddleware 写入线程上下文）。 */
public class CurrentUser {
  public Long id;
  public String username;
  public String displayName;
  public String role;

  public CurrentUser() {}

  public CurrentUser(Long id, String username, String displayName, String role) {
    this.id = id;
    this.username = username;
    this.displayName = displayName;
    this.role = role;
  }
}
