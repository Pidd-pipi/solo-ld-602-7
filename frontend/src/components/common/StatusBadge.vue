<script setup lang="ts">
import { computed } from "vue";
import { DispatchStatusText, DispatchStatusType, type DispatchStatus } from "../../constants/DispatchStatus";
import { ShelterStatusText, ShelterStatusType, type ShelterStatus } from "../../constants/ShelterStatus";
import { AllocationStageText, type AllocationStage } from "../../constants/LedgerDirection";

type Domain = "dispatch" | "shelter" | "stage" | "plain";

const props = defineProps<{
  value: string;
  domain?: Domain;
}>();

const meta = computed(() => {
  const domain: Domain = props.domain ?? inferDomain(props.value);
  switch (domain) {
    case "dispatch":
      return {
        text: DispatchStatusText[props.value as DispatchStatus] ?? props.value,
        type: DispatchStatusType[props.value as DispatchStatus] ?? "info"
      };
    case "shelter":
      return {
        text: ShelterStatusText[props.value as ShelterStatus] ?? props.value,
        type: ShelterStatusType[props.value as ShelterStatus] ?? "info"
      };
    case "stage":
      return {
        text: AllocationStageText[props.value as AllocationStage] ?? props.value,
        type: props.value === "OUT" ? "primary" : props.value === "RETURNED" ? "info" : "warning"
      };
    default:
      return { text: props.value, type: "info" as const };
  }
});

function inferDomain(v: string): Domain {
  if (v in DispatchStatusText) return "dispatch";
  if (v in ShelterStatusText) return "shelter";
  if (v in AllocationStageText) return "stage";
  return "plain";
}
</script>

<template>
  <el-tag :type="meta.type" size="small" effect="light" disable-transitions>{{ meta.text }}</el-tag>
</template>
