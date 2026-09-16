import type { Warehouse } from "../types/Warehouse";

export const createDefaultWarehouse = (overrides: Partial<Warehouse> = {}): Warehouse => ({
  id: 0,
  name: "",
  district: "",
  address: "",
  manager_id: null,
  capacity_level: "L2",
  contact_phone: "",
  status: "ACTIVE",
  ...overrides
});

export const createWarehouseForm = createDefaultWarehouse;
export const createWarehouseResponse = createDefaultWarehouse;
