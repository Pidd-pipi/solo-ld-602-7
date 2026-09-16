import { request } from "../utils/request";
import type { DashboardOverview } from "../types/Dashboard";

export const getDashboardOverview = (): Promise<DashboardOverview> =>
  request<DashboardOverview>("/api/dashboard/overview");
