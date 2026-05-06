<template>
  <div>
    <div
      v-if="error"
      role="alert"
      class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm"
    >
      {{ error }}
    </div>

    <div
      v-if="loading"
      data-testid="booth-locations-loading"
      class="text-sm text-gray-500 py-6 text-center"
    >
      {{ t("common.loading") }}
    </div>

    <div v-else-if="sponsors.length === 0" class="text-sm text-gray-500 py-6 text-center">
      {{ t("boothPlan.sponsors.noSponsors") }}
    </div>

    <table v-else class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50">
        <tr>
          <th
            scope="col"
            class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
          >
            {{ t("boothPlan.sponsors.columns.location") }}
          </th>
          <th
            scope="col"
            class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
          >
            {{ t("boothPlan.sponsors.columns.company") }}
          </th>
          <th
            scope="col"
            class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
          >
            {{ t("boothPlan.sponsors.columns.pack") }}
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-200 bg-white">
        <tr v-for="sponsor in sponsors" :key="sponsor.id">
          <td class="px-4 py-2 text-sm">
            <span
              v-if="sponsor.boothLocation"
              class="inline-flex items-center px-2 py-0.5 rounded bg-blue-100 text-blue-800 font-medium"
            >
              {{ sponsor.boothLocation }}
            </span>
            <span v-else class="text-gray-500 italic">
              {{ t("boothPlan.sponsors.noLocation") }}
            </span>
          </td>
          <td class="px-4 py-2 text-sm">
            <a
              :href="`/orgs/${orgSlug}/events/${eventSlug}/sponsors/${sponsor.id}`"
              class="text-blue-600 hover:underline"
            >
              {{ sponsor.companyName }}
            </a>
          </td>
          <td class="px-4 py-2 text-sm">
            <span
              class="inline-flex items-center px-2 py-0.5 rounded font-medium"
              :class="
                sponsor.packName === 'Pack Gold'
                  ? 'bg-yellow-100 text-yellow-800'
                  : 'bg-gray-100 text-gray-800'
              "
            >
              {{ sponsor.packName }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { BoothSponsor } from "~/composables/useBoothSponsors";

defineProps<{
  sponsors: BoothSponsor[];
  loading: boolean;
  error: string | null;
  orgSlug: string;
  eventSlug: string;
}>();

const { t } = useI18n();
</script>
