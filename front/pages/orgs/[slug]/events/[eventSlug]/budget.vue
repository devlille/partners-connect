<template>
  <Dashboard :main-links="eventLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div>
        <BackButton :to="`/orgs/${orgSlug}/events/${eventSlug}`" label="Retour" />
        <PageTitle>{{ t('budget.title') }} — {{ eventName }}</PageTitle>
      </div>
    </div>

    <div class="p-6 space-y-6">
      <!-- Loading: full skeleton -->
      <TableSkeleton v-if="loading" :columns="4" :rows="6" />

      <!-- Error banner -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <template v-else-if="budget">
        <!-- Pack filter -->
        <div class="flex items-center gap-3">
          <label for="budget-pack-filter" class="text-sm font-medium text-gray-700">
            {{ t('budget.packFilterLabel') }}
          </label>
          <USelect
            id="budget-pack-filter"
            v-model="selectedPackId"
            :items="packOptions"
            class="w-64"
          />
        </div>

        <!-- Totals card -->
        <BudgetTotalsCard :totals="displayedTotals" :currency="budget.currency" />

        <!-- Partnership list -->
        <div v-if="displayedPartnerships.length === 0" class="text-center py-12 text-gray-500">
          {{ t('budget.empty') }}
        </div>
        <UTable v-else :data="displayedPartnerships" :columns="columns" />
      </template>
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { h } from "vue";
import { getEventBySlug, getOrgsEventsBudget, type BudgetTotalsSchema, type EventBudgetSchema, type PartnershipBudgetItemSchema } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import { formatPrice } from "~/utils/formatPrice";
import BudgetTotalsCard from "~/components/budget/TotalsCard.vue";
import StatusBadge from "~/components/budget/StatusBadge.vue";

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const { t } = useI18n();
const { footerLinks } = useDashboardLinks();
const { getOrgSlug, getEventSlug } = useRouteParams();

const orgSlug = computed(() => getOrgSlug());
const eventSlug = computed(() => getEventSlug());
const { eventLinks } = useEventLinks(orgSlug.value, eventSlug.value);

interface BudgetRow extends PartnershipBudgetItemSchema {
  pack_name: string;
  pack_id: string;
}

const budget = ref<EventBudgetSchema | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);
const eventName = ref("");
const selectedPackId = ref<string | null>(null);

const packOptions = computed(() => {
  const all = { label: t("budget.allPacks"), value: null as string | null };
  const packs = (budget.value?.packs ?? []).map((p) => ({
    label: p.pack_name,
    value: p.pack_id,
  }));
  return [all, ...packs];
});

const selectedPack = computed(() => {
  if (!selectedPackId.value || !budget.value) return null;
  return budget.value.packs.find((p) => p.pack_id === selectedPackId.value) ?? null;
});

const displayedTotals = computed<BudgetTotalsSchema>(() => {
  if (selectedPack.value) return selectedPack.value.totals;
  return budget.value?.totals ?? {
    paid: 0,
    validated: 0,
    validated_minus_paid: 0,
    total: 0,
    total_minus_validated: 0,
  };
});

const displayedPartnerships = computed<BudgetRow[]>(() => {
  if (!budget.value) return [];
  if (selectedPack.value) {
    return selectedPack.value.partnerships.map((p) => ({
      ...p,
      pack_name: selectedPack.value!.pack_name,
      pack_id: selectedPack.value!.pack_id,
    }));
  }
  return budget.value.packs.flatMap((pack) =>
    pack.partnerships.map((p) => ({
      ...p,
      pack_name: pack.pack_name,
      pack_id: pack.pack_id,
    })),
  );
});

const columns = computed(() => {
  const cols: any[] = [];
  if (!selectedPackId.value) {
    cols.push({
      header: t("budget.columns.pack"),
      accessorKey: "pack_name",
    });
  }
  cols.push(
    {
      header: t("budget.columns.company"),
      accessorKey: "company_name",
    },
    {
      header: t("budget.columns.price"),
      accessorKey: "price_applied",
      cell: (info: any) =>
        h(
          "div",
          { class: "font-medium tabular-nums" },
          formatPrice(info.getValue() as number, budget.value?.currency ?? "EUR"),
        ),
    },
    {
      header: t("budget.columns.status"),
      accessorKey: "status",
      cell: (info: any) => h(StatusBadge, { status: info.getValue() }),
    },
  );
  return cols;
});

async function loadBudget() {
  try {
    loading.value = true;
    error.value = null;
    const [eventResponse, budgetResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      getOrgsEventsBudget(orgSlug.value, eventSlug.value),
    ]);
    eventName.value = eventResponse.data.event.name;
    budget.value = budgetResponse.data;
  } catch (err) {
    console.error("Failed to load budget:", err);
    error.value = t("budget.error");
  } finally {
    loading.value = false;
  }
}

onMounted(loadBudget);

watch([orgSlug, eventSlug], () => {
  selectedPackId.value = null;
  loadBudget();
});

useHead({
  title: computed(() => `${t("budget.title")} — ${eventName.value || "Événement"} | DevLille`),
});
</script>
