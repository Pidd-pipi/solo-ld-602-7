import type { Shelter } from "../types/Shelter";

export const createDefaultShelter = (overrides: Partial<Shelter> = {}): Shelter => ({
  id: 1 as never,
  name: "name 1" as never,
  district: "district 1" as never,
  capacity: 92 as never,
  current_population: 92 as never,
  contact_person: "contact person 1" as never,
  risk_level: "LOW" as never,
  open_status: "SUBMITTED" as never,
  ...overrides
});

export const createShelterForm = createDefaultShelter;
export const createShelterResponse = createDefaultShelter;
