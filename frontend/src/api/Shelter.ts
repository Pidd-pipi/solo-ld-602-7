import { request } from "../utils/request";
import type { Shelter } from "../types/Shelter";

export interface ReceiveRecord {
  id: number;
  status: string;
  priority: string;
  dispatched_at: string | null;
  received_at: string | null;
  warehouse_name: string;
  total_requested: number;
}

export const listShelters = (): Promise<Shelter[]> => request<Shelter[]>("/api/shelters");

export const updateShelter = (id: number, body: Partial<Shelter>): Promise<{ updated: boolean }> =>
  request<{ updated: boolean }>(`/api/shelters/${id}`, {
    method: "PUT",
    body: JSON.stringify(body)
  });

export const listReceiveRecords = (shelterId: number): Promise<ReceiveRecord[]> =>
  request<ReceiveRecord[]>(`/api/shelters/${shelterId}/receive-records`);
