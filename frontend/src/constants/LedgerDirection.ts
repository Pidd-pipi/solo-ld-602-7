/** 库存流水方向与批次占用阶段，与后端 LedgerDirection 同名同义。 */
export const LedgerDirection = ["HOLD", "RELEASE", "OUTBOUND", "RETURN"] as const;
export type LedgerDirection = (typeof LedgerDirection)[number];

export const LedgerDirectionText: Record<LedgerDirection, string> = {
  HOLD: "申请占用",
  RELEASE: "驳回释放",
  OUTBOUND: "出库扣减",
  RETURN: "拒签/撤销回补"
};

export const AllocationStage = ["HELD", "OUT", "RETURNED"] as const;
export type AllocationStage = (typeof AllocationStage)[number];

export const AllocationStageText: Record<AllocationStage, string> = {
  HELD: "占用中",
  OUT: "已出库",
  RETURNED: "已回补"
};
