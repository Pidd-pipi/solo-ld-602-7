import { defineStore } from "pinia";
import { getDashboardOverview } from "../api/Dashboard";
import type { DashboardOverview } from "../types/Dashboard";

const EMPTY: DashboardOverview = {
  stock: { total_qty: 0, held_qty: 0, available_qty: 0, warehouse_count: 0, batch_count: 0 },
  orders: {
    pending_approval: 0,
    approved_waiting_outbound: 0,
    in_transit: 0,
    received: 0,
    closed_abnormal: 0,
    total_orders: 0
  },
  nearExpireBatches: 0,
  nearExpireDays: 30,
  sheltersOpen: 0,
  byItem: [],
  nearExpireList: [],
  recentLedger: [],
  statusMatrix: []
};

/** 态势大屏数据：每次进入或动作后刷新，数量随调拨状态实时变化。 */
export const useDashboardStore = defineStore("dashboard", {
  state: () => ({
    overview: { ...EMPTY } as DashboardOverview,
    loading: false,
    loadedAt: ""
  }),
  actions: {
    async load() {
      this.loading = true;
      try {
        this.overview = await getDashboardOverview();
        this.loadedAt = new Date().toLocaleString("zh-CN", { hour12: false });
      } finally {
        this.loading = false;
      }
    }
  }
});
