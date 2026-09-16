import { SupplyCategoryText } from "./SupplyCategory";
import { DispatchStatusText } from "./DispatchStatus";
import { ShelterStatusText } from "./ShelterStatus";
import { LedgerDirectionText, AllocationStageText } from "./LedgerDirection";

/** 统一文案出口，多页面/组件共同依赖，改一处全局生效。 */
export const STATUS_TEXT = {
  SupplyCategory: SupplyCategoryText,
  DispatchStatus: DispatchStatusText,
  ShelterStatus: ShelterStatusText,
  LedgerDirection: LedgerDirectionText,
  AllocationStage: AllocationStageText
};

export const PRIORITY_TEXT: Record<string, string> = {
  LOW: "低",
  NORMAL: "常规",
  HIGH: "紧急",
  CRITICAL: "特急"
};

export const RISK_TEXT: Record<string, string> = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",
  CRITICAL: "严重"
};
