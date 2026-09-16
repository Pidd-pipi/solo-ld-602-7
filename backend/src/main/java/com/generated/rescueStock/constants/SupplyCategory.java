package com.generated.rescueStock.constants;

/** 物资分类，前后端与种子数据共用同一取值域。 */
public enum SupplyCategory {
  FOOD, WATER, MEDICAL, SHELTER, RESCUE_TOOL;

  public String label() {
    return switch (this) {
      case FOOD -> "食品";
      case WATER -> "饮水";
      case MEDICAL -> "医疗";
      case SHELTER -> "安置";
      case RESCUE_TOOL -> "救援工具";
    };
  }
}
