export interface DispatchOrder {
  id: number;
  event_id: number;
  source_warehouse_id: number;
  shelter_id: number;
  priority: string;
  status: string;
  requested_by: string;
  approved_by: string;
  dispatched_at: string;
}
