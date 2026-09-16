package com.generated.rescueStock.constants;

/** 避难点开放状态。 */
public enum ShelterStatus {
  CLOSED, STANDBY, OPEN, FULL;

  public String label() {
    return switch (this) {
      case CLOSED -> "关闭";
      case STANDBY -> "待命";
      case OPEN -> "开放";
      case FULL -> "满载";
    };
  }
}
