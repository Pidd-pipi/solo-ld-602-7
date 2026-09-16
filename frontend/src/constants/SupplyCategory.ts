export const SupplyCategory = ["FOOD", "WATER", "MEDICAL", "SHELTER", "RESCUE_TOOL"] as const;
export type SupplyCategory = (typeof SupplyCategory)[number];

export const SupplyCategoryText: Record<SupplyCategory, string> = {
  FOOD: "食品",
  WATER: "饮水",
  MEDICAL: "医疗",
  SHELTER: "安置",
  RESCUE_TOOL: "救援工具"
};
