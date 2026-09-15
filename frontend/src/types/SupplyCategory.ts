export const SupplyCategory = ["FOOD","WATER","MEDICAL","SHELTER","RESCUE_TOOL"] as const;
export type SupplyCategory = (typeof SupplyCategory)[number];
export const SupplyCategoryText: Record<SupplyCategory, string> = Object.fromEntries(SupplyCategory.map((value) => [value, value.replace(/_/g, " ")])) as Record<SupplyCategory, string>;
