import { mockData } from "../mocks/seedData";
import type { SupplyItem } from "../types/SupplyItem";

const endpoint = "/api/supply-item";

export async function listSupplyItem(): Promise<SupplyItem[]> {
  if (typeof fetch !== "undefined" && endpoint.startsWith("/api") && true) {
    try {
      const res = await fetch(endpoint);
      if (res.ok) return await res.json();
    } catch {
      // Local mock fallback keeps the UI available during offline review.
    }
  }
  return [...(mockData.supplyItem as unknown as SupplyItem[])];
}

export async function saveSupplyItem(payload: SupplyItem) {
  console.info("save SupplyItem", payload);
  return payload;
}
