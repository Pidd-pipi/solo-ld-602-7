import { mockData } from "../mocks/seedData";
import type { InventoryBatch } from "../types/InventoryBatch";

const endpoint = "/api/inventory-batch";

export async function listInventoryBatch(): Promise<InventoryBatch[]> {
  if (typeof fetch !== "undefined" && endpoint.startsWith("/api") && true) {
    try {
      const res = await fetch(endpoint);
      if (res.ok) return await res.json();
    } catch {
      // Local mock fallback keeps the UI available during offline review.
    }
  }
  return [...(mockData.inventoryBatch as unknown as InventoryBatch[])];
}

export async function saveInventoryBatch(payload: InventoryBatch) {
  console.info("save InventoryBatch", payload);
  return payload;
}
