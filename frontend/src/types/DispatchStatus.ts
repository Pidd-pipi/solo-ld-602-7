export const DispatchStatus = ["DRAFT","SUBMITTED","APPROVED","DISPATCHED","RECEIVED","REJECTED"] as const;
export type DispatchStatus = (typeof DispatchStatus)[number];
export const DispatchStatusText: Record<DispatchStatus, string> = Object.fromEntries(DispatchStatus.map((value) => [value, value.replace(/_/g, " ")])) as Record<DispatchStatus, string>;
