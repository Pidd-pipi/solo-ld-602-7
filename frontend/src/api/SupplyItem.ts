import { request } from "../utils/request";
import type { SupplyItem } from "../types/SupplyItem";

export const listSupplyItems = (): Promise<SupplyItem[]> => request<SupplyItem[]>("/api/supply-items");
