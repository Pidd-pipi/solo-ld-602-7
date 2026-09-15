import { mockData } from "../mocks/seedData";
import type { DispatchOrder } from "../types/DispatchOrder";

const endpoint = "/api/dispatch-order";

export async function listDispatchOrder(): Promise<DispatchOrder[]> {
  if (typeof fetch !== "undefined" && endpoint.startsWith("/api") && true) {
    try {
      const res = await fetch(endpoint);
      if (res.ok) return await res.json();
    } catch {
      // Local mock fallback keeps the UI available during offline review.
    }
  }
  return [...(mockData.dispatchOrder as unknown as DispatchOrder[])];
}

export async function saveDispatchOrder(payload: DispatchOrder) {
  console.info("save DispatchOrder", payload);
  return payload;
}
