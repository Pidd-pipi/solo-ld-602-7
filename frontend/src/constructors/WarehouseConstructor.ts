import type { Warehouse } from "../types/Warehouse";

export const createDefaultWarehouse = (overrides: Partial<Warehouse> = {}): Warehouse => ({
  id: 1 as never,
  name: "name 1" as never,
  district: "district 1" as never,
  address: "address 1" as never,
  manager_id: 1 as never,
  capacity_level: 92 as never,
  contact_phone: "13800000001" as never,
  status: "SUBMITTED" as never,
  ...overrides
});

export const createWarehouseForm = createDefaultWarehouse;
export const createWarehouseResponse = createDefaultWarehouse;
