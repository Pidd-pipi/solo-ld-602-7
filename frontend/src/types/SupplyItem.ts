import type { SupplyCategory } from "../constants/SupplyCategory";

export interface SupplyItem {
  id: number;
  sku_code: string;
  name: string;
  category: SupplyCategory;
  unit: string;
  safety_stock: number;
  expire_days: number;
  storage_requirement: string;
}
