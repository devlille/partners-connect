<template>
  <UBadge :color="badgeColor" variant="subtle" size="sm">
    {{ label }}
  </UBadge>
</template>

<script setup lang="ts">
import type { PartnershipBudgetItemSchemaStatus } from "~/utils/api";

const props = defineProps<{
  status: PartnershipBudgetItemSchemaStatus;
}>();

const { t } = useI18n();

const badgeColor = computed(() => {
  switch (props.status) {
    case "paid":
      return "success";
    case "validated":
      return "primary";
    case "submitted":
      return "neutral";
    default:
      return "neutral";
  }
});

const label = computed(() => t(`budget.status.${props.status}`));
</script>
