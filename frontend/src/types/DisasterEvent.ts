export interface DisasterEvent {
  id: number;
  title: string;
  district: string;
  level: string;
  status: string;
  created_at: string;
  dispatch_count?: number;
}
