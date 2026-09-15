export const ShelterStatus = ["CLOSED","STANDBY","OPEN","FULL"] as const;
export type ShelterStatus = (typeof ShelterStatus)[number];
export const ShelterStatusText: Record<ShelterStatus, string> = Object.fromEntries(ShelterStatus.map((value) => [value, value.replace(/_/g, " ")])) as Record<ShelterStatus, string>;
