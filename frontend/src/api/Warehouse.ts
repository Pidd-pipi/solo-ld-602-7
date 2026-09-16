import { request } from "../utils/request";
import type { Warehouse } from "../types/Warehouse";

export const listWarehouses = (): Promise<Warehouse[]> => request<Warehouse[]>("/api/warehouses");
