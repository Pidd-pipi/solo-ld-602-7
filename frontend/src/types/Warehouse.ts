export interface Warehouse {
  id: number;
  name: string;
  district: string;
  address: string;
  manager_id: number | null;
  manager_name?: string;
  capacity_level: string;
  contact_phone: string;
  status: string;
}
