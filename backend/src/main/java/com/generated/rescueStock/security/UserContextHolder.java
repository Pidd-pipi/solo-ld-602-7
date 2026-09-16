package com.generated.rescueStock.security;

/** 线程级当前用户持有器，service 层据此记录操作人，且永远不信任前端传入的操作人。 */
public final class UserContextHolder {
  private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

  public static void set(CurrentUser user) { HOLDER.set(user); }
  public static CurrentUser get() { return HOLDER.get(); }
  public static String actorName() {
    CurrentUser u = HOLDER.get();
    return u == null ? "system" : u.username;
  }
  public static Long userId() {
    CurrentUser u = HOLDER.get();
    return u == null ? null : u.id;
  }
  public static void clear() { HOLDER.remove(); }

  private UserContextHolder() {}
}
