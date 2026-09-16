export const ShelterStatus = ["CLOSED", "STANDBY", "OPEN", "FULL"] as const;
export type ShelterStatus = (typeof ShelterStatus)[number];

export const ShelterStatusText: Record<ShelterStatus, string> = {
  CLOSED: "关闭",
  STANDBY: "待命",
  OPEN: "开放",
  FULL: "满载"
};

export const ShelterStatusType: Record<ShelterStatus, "info" | "warning" | "success" | "danger"> = {
  CLOSED: "info",
  STANDBY: "warning",
  OPEN: "success",
  FULL: "danger"
};
