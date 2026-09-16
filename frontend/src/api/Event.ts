import { request } from "../utils/request";
import type { DisasterEvent } from "../types/DisasterEvent";

export const listEvents = (): Promise<DisasterEvent[]> => request<DisasterEvent[]>("/api/events");

export const createEvent = (body: { title: string; district: string; level?: string }): Promise<{ id: number }> =>
  request<{ id: number }>("/api/events", { method: "POST", body: JSON.stringify(body) });
