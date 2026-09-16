import { request } from "../utils/request";
import type { DispatchOrder, DispatchDetail, BatchAllocation, DispatchLine, TimelineEntry } from "../types/DispatchOrder";
import type { ApplyDispatchPayload } from "../types/api";

export interface ListParams {
  status?: string;
  shelterId?: number;
}

export const listDispatchOrders = (params: ListParams = {}): Promise<DispatchOrder[]> => {
  const qs = new URLSearchParams();
  if (params.status) qs.set("status", params.status);
  if (params.shelterId) qs.set("shelterId", String(params.shelterId));
  const suffix = qs.toString() ? `?${qs.toString()}` : "";
  return request<DispatchOrder[]>(`/api/dispatch-orders${suffix}`);
};

export const getDispatchDetail = (id: number): Promise<DispatchDetail> =>
  request<DispatchDetail>(`/api/dispatch-orders/${id}`);

export const getDispatchLines = (id: number): Promise<DispatchLine[]> =>
  request<DispatchLine[]>(`/api/dispatch-orders/${id}/lines`);

export const getDispatchAllocations = (id: number): Promise<BatchAllocation[]> =>
  request<BatchAllocation[]>(`/api/dispatch-orders/${id}/allocations`);

export const getDispatchTimeline = (id: number): Promise<TimelineEntry[]> =>
  request<TimelineEntry[]>(`/api/dispatch-orders/${id}/timeline`);

export interface ApplyResult {
  id: number;
  idempotent?: boolean;
}

/** 申请调拨（先占可用库存）。同一 requestId 重复提交幂等返回，不重复占库。 */
export const applyDispatch = (payload: ApplyDispatchPayload): Promise<DispatchOrder> =>
  request<DispatchOrder>("/api/dispatch-orders", {
    method: "POST",
    body: JSON.stringify(payload)
  });

const action = (id: number, name: string, reason?: string): Promise<{ id: number }> =>
  request<{ id: number }>(`/api/dispatch-orders/${id}/${name}`, {
    method: "POST",
    body: JSON.stringify({ reason: reason ?? "" })
  });

export const approveDispatch = (id: number) => action(id, "approve");
export const rejectDispatch = (id: number, reason: string) => action(id, "reject", reason);
export const outboundDispatch = (id: number) => action(id, "outbound");
export const receiveDispatch = (id: number) => action(id, "receive");
export const refuseDispatch = (id: number, reason: string) => action(id, "refuse", reason);
export const cancelDispatch = (id: number, reason: string) => action(id, "cancel", reason);
