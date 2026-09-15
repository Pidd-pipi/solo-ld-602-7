import type { SupplyItem } from "../types/SupplyItem";

export const createDefaultSupplyItem = (overrides: Partial<SupplyItem> = {}): SupplyItem => ({
  id: 1 as never,
  sku_code: "sku code 1" as never,
  name: "name 1" as never,
  category: "WATER" as never,
  unit: "unit 1" as never,
  safety_stock: "safety stock 1" as never,
  expire_days: "expire days 1" as never,
  storage_requirement: "storage requirement 1" as never,
  ...overrides
});

export const createSupplyItemForm = createDefaultSupplyItem;
export const createSupplyItemResponse = createDefaultSupplyItem;
