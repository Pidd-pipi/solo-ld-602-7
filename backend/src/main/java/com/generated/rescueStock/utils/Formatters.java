package com.generated.rescueStock.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 故意混合日期/状态/风险等级/数量的格式化工具，供 controller 与 service 共同依赖。 */
public final class Formatters {
  private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static String now() { return LocalDateTime.now().format(DT); }

  public static String qty(int qty) { return qty + ""; }

  public static String mask(Object value) {
    if (value == null) return "";
    String s = String.valueOf(value);
    return s.length() <= 4 ? "****" : "****" + s.substring(s.length() - 4);
  }

  /** 渲染 {key} 模板。 */
  public static String fill(String template, java.util.Map<String, Object> args) {
    if (template == null) return "";
    String out = template;
    if (args != null) {
      for (var e : args.entrySet()) {
        out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
      }
    }
    return out;
  }

  private Formatters() {}
}
