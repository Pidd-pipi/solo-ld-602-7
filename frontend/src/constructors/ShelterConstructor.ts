import type { Shelter } from "../types/Shelter";

export const createDefaultShelter = (overrides: Partial<Shelter> = {}): Shelter => ({
  id: 0,
  name: "",
  district: "",
  capacity: 0,
  current_population: 0,
  contact_person: "",
  contact_phone: "",
  risk_level: "LOW",
  open_status: "STANDBY",
  ...overrides
});

export const createShelterForm = createDefaultShelter;
export const createShelterResponse = createDefaultShelter;
