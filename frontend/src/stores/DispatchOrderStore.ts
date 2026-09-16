import { defineStore } from "pinia";
import {
  listDispatchOrders,
  applyDispatch,
  approveDispatch,
  rejectDispatch,
  outboundDispatch,
  receiveDispatch,
  refuseDispatch,
  cancelDispatch
} from "../api/DispatchOrder";
import type { DispatchOrder } from "../types/DispatchOrder";
import type { ApplyDispatchPayload } from "../types/api";
import { DispatchStatus } from "../constants/DispatchStatus";

interface State {
  rows: DispatchOrder[];
  statusFilter: string;
  loading: boolean;
  lastAppliedId: number | null;
  lastIdempotent: boolean;
}

/**
 * 调拨列表 store：所有写动作成功后都重新拉取列表，
 * 保证“调拨列表 / 审批详情 / 态势数量”随状态同步变化。
 */
export const useDispatchStore = defineStore("dispatch", {
  state: (): State => ({
    rows: [],
    statusFilter: "",
    loading: false,
    lastAppliedId: null,
    lastIdempotent: false
  }),
  getters: {
    filtered(state): DispatchOrder[] {
      return state.statusFilter
        ? state.rows.filter((r) => r.status === state.statusFilter)
        : state.rows;
    },
    countByStatus(state): Record<string, number> {
      const map: Record<string, number> = {};
      for (const s of DispatchStatus) map[s] = 0;
      for (const r of state.rows) map[r.status] = (map[r.status] ?? 0) + 1;
      return map;
    },
    pendingCount(): number {
      return this.countByStatus["SUBMITTED"] ?? 0;
    }
  },
  actions: {
    async load() {
      this.loading = true;
      try {
        // 始终拉全量，过滤交给 getter，保证状态矩阵准确
        this.rows = await listDispatchOrders();
      } finally {
        this.loading = false;
      }
    },
    setFilter(status: string) {
      this.statusFilter = status;
    },
    async apply(payload: ApplyDispatchPayload) {
      const order = await applyDispatch(payload);
      this.lastAppliedId = order.id;
      // 后端对重复 requestId 返回原单；这里以状态判断是否幂等（原单通常已离开 SUBMITTED 也可能仍在）
      this.lastIdempotent = this.rows.some((r) => r.request_id === payload.requestId);
      await this.load();
      return order;
    },
    async approve(id: number) {
      await approveDispatch(id);
      await this.load();
    },
    async reject(id: number, reason: string) {
      await rejectDispatch(id, reason);
      await this.load();
    },
    async outbound(id: number) {
      await outboundDispatch(id);
      await this.load();
    },
    async receive(id: number) {
      await receiveDispatch(id);
      await this.load();
    },
    async refuse(id: number, reason: string) {
      await refuseDispatch(id, reason);
      await this.load();
    },
    async cancel(id: number, reason: string) {
      await cancelDispatch(id, reason);
      await this.load();
    }
  }
});
