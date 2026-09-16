import type { SupplyItem } from "../types/SupplyItem";

export const createDefaultSupplyItem = (overrides: Partial<SupplyItem> = {}): SupplyItem => ({
  id: 0,
  sku_code: "",
  name: "",
  category: "WATER",
  unit: "",
  safety_stock: 0,
  expire_days: 0,
  storage_requirement: "",
  ...overrides
});

export const createSupplyItemForm = createDefaultSupplyItem;
export const createSupplyItemResponse = createDefaultSupplyItem;
