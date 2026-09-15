import { mockData } from "../mocks/seedData";
import type { Shelter } from "../types/Shelter";

const endpoint = "/api/shelter";

export async function listShelter(): Promise<Shelter[]> {
  if (typeof fetch !== "undefined" && endpoint.startsWith("/api") && true) {
    try {
      const res = await fetch(endpoint);
      if (res.ok) return await res.json();
    } catch {
      // Local mock fallback keeps the UI available during offline review.
    }
  }
  return [...(mockData.shelter as unknown as Shelter[])];
}

export async function saveShelter(payload: Shelter) {
  console.info("save Shelter", payload);
  return payload;
}
