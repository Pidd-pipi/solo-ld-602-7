export interface ApplyLine {
  supplyItemId: number;
  requestedQty: number;
}

export interface ApplyDispatchPayload {
  requestId: string;
  eventId: number | null;
  sourceWarehouseId: number;
  shelterId: number;
  priority: string;
  remark?: string;
  lines: ApplyLine[];
}

export interface LoginUser {
  id: number;
  username: string;
  displayName: string;
  role: "STREET_ADMIN" | "WAREHOUSE_KEEPER" | "APPROVER" | "OBSERVER";
}

export interface LoginResult {
  token: string;
  user: LoginUser;
}
