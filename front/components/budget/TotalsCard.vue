<template>
  <div class="bg-white rounded-lg shadow p-6 border border-gray-200">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Paid -->
      <div>
        <div class="text-sm font-medium text-gray-500">{{ t('budget.totals.paid') }}</div>
        <div class="mt-1 text-3xl font-bold text-green-600">
          {{ formatPrice(totals.paid, currency) }}
        </div>
      </div>

      <!-- Validated + derived (to come) -->
      <div>
        <div class="text-sm font-medium text-gray-500">{{ t('budget.totals.validated') }}</div>
        <div class="mt-1 text-3xl font-bold text-blue-600">
          {{ formatPrice(totals.validated, currency) }}
        </div>
        <div class="mt-1 text-xs text-gray-500">
          ↑ {{ formatPrice(totals.validated_minus_paid, currency) }} {{ t('budget.totals.toCome') }}
        </div>
      </div>

      <!-- Total + derived (pipeline) -->
      <div>
        <div class="text-sm font-medium text-gray-500">{{ t('budget.totals.total') }}</div>
        <div class="mt-1 text-3xl font-bold text-gray-900">
          {{ formatPrice(totals.total, currency) }}
        </div>
        <div class="mt-1 text-xs text-gray-500">
          ↑ {{ formatPrice(totals.total_minus_validated, currency) }} {{ t('budget.totals.pipeline') }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BudgetTotalsSchema } from "~/utils/api";
import { formatPrice } from "~/utils/formatPrice";

defineProps<{
  totals: BudgetTotalsSchema;
  currency: string;
}>();

const { t } = useI18n();
</script>
