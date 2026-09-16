import type { ShelterStatus } from "../constants/ShelterStatus";

export interface Shelter {
  id: number;
  name: string;
  district: string;
  capacity: number;
  current_population: number;
  contact_person: string;
  contact_phone: string;
  risk_level: string;
  open_status: ShelterStatus;
  inbound_in_transit?: number;
  received_orders?: number;
}
