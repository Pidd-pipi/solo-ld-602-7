import { mockData } from "../mocks/seedData";
import type { Warehouse } from "../types/Warehouse";

const endpoint = "/api/warehouse";

export async function listWarehouse(): Promise<Warehouse[]> {
  if (typeof fetch !== "undefined" && endpoint.startsWith("/api") && true) {
    try {
      const res = await fetch(endpoint);
      if (res.ok) return await res.json();
    } catch {
      // Local mock fallback keeps the UI available during offline review.
    }
  }
  return [...(mockData.warehouse as unknown as Warehouse[])];
}

export async function saveWarehouse(payload: Warehouse) {
  console.info("save Warehouse", payload);
  return payload;
}
