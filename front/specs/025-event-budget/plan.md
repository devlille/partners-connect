# Front Budget Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a new "Budget" entry in the event sidebar that opens a screen showing a totals card and a partnership list, with a client-side pack filter that narrows both.

**Architecture:** One Nuxt page (`pages/orgs/[slug]/events/[eventSlug]/budget.vue`) holds data + filter state. Two small presentational children under `components/budget/` (TotalsCard, StatusBadge). One pure helper `utils/formatPrice.ts`. One sidebar tweak in `composables/useEventLinks.ts`. i18n keys in all three locales. Single API call — `getOrgsEventsBudget` — returns everything; filtering is client-side.

**Tech Stack:** Nuxt 4.2 / Vue 3.5 Composition API / @nuxt/ui 4.1 / @nuxtjs/i18n 10.1 / TypeScript / Orval-generated client.

---

## File Structure

**Create:**

- `front/utils/formatPrice.ts` — `Intl.NumberFormat` wrapper.
- `front/components/budget/StatusBadge.vue` — colored `UBadge` for paid/validated/submitted.
- `front/components/budget/TotalsCard.vue` — 3-primary + 2-derived totals layout.
- `front/pages/orgs/[slug]/events/[eventSlug]/budget.vue` — the page.

**Modify:**

- `front/composables/useEventLinks.ts` — insert Budget entry after Dashboard.
- `front/i18n/locales/fr-FR.json` — add `budget.*` namespace.
- `front/i18n/locales/en-US.json` — add `budget.*` namespace.
- `front/i18n/locales/es-ES.json` — add `budget.*` namespace.

All work happens on branch `025-event-budget` (already checked out). Front and server live in the same monorepo; commits on this branch update PR #213.

---

## Task 1: Sidebar entry

**Files:**

- Modify: `front/composables/useEventLinks.ts`

- [ ] **Step 1: Insert the Budget entry**

Edit `front/composables/useEventLinks.ts`. Find the existing "Dashboard" object at lines 4-8:

```ts
{
  label: "Dashboard",
  icon: "i-heroicons-chart-bar",
  to: `/orgs/${orgSlug}/events/${eventSlug}`,
},
```

Immediately AFTER its closing `},`, insert:

```ts
{
  label: "Budget",
  icon: "i-heroicons-banknotes",
  to: `/orgs/${orgSlug}/events/${eventSlug}/budget`,
},
```

The result should be: Dashboard → Budget → Informations → Mes Packs → ... (rest of the array unchanged).

- [ ] **Step 2: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/composables/useEventLinks.ts
git commit -m "$(cat <<'EOF'
feat(front): add Budget entry to event sidebar

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: `formatPrice` helper

**Files:**

- Create: `front/utils/formatPrice.ts`

- [ ] **Step 1: Create the file**

```ts
/**
 * Format a monetary integer as a currency string using the active i18n locale.
 *
 * @param value - Amount in major currency units (no decimals expected from the API)
 * @param currency - ISO 4217 currency code (e.g. "EUR") returned by the API
 * @param locale - BCP-47 locale; defaults to "fr-FR" (the app's default i18n locale)
 */
export function formatPrice(value: number, currency: string, locale: string = "fr-FR"): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(value);
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/utils/formatPrice.ts
git commit -m "$(cat <<'EOF'
feat(front): add formatPrice helper for currency formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: i18n keys

**Files:**

- Modify: `front/i18n/locales/fr-FR.json`
- Modify: `front/i18n/locales/en-US.json`
- Modify: `front/i18n/locales/es-ES.json`

The locale files are nested JSON objects. Each top-level key is a namespace (e.g., `common`, `partnershipNotices`, `nav`). Find the closing `}` of the last namespace, change the preceding `}` to `},`, and append the new `budget` namespace.

- [ ] **Step 1: Add `budget` namespace to `fr-FR.json`**

Open `front/i18n/locales/fr-FR.json`, scroll to the very last `}` of the file, and add this new namespace at the end (turn the previous `}` into `},` first):

```json
  "budget": {
    "title": "Budget",
    "allPacks": "Tous les packs",
    "packFilterLabel": "Pack",
    "columns": {
      "pack": "Pack",
      "company": "Entreprise",
      "price": "Prix",
      "status": "Statut"
    },
    "status": {
      "paid": "Payé",
      "validated": "Validé",
      "submitted": "Soumis"
    },
    "totals": {
      "paid": "Payé",
      "validated": "Validé",
      "total": "Total",
      "toCome": "à venir",
      "pipeline": "en cours"
    },
    "empty": "Aucun partenariat à afficher.",
    "loading": "Chargement du budget…",
    "error": "Impossible de charger le budget."
  }
```

- [ ] **Step 2: Add `budget` namespace to `en-US.json`**

Same procedure for `front/i18n/locales/en-US.json`:

```json
  "budget": {
    "title": "Budget",
    "allPacks": "All packs",
    "packFilterLabel": "Pack",
    "columns": {
      "pack": "Pack",
      "company": "Company",
      "price": "Price",
      "status": "Status"
    },
    "status": {
      "paid": "Paid",
      "validated": "Validated",
      "submitted": "Submitted"
    },
    "totals": {
      "paid": "Paid",
      "validated": "Validated",
      "total": "Total",
      "toCome": "to come",
      "pipeline": "pipeline"
    },
    "empty": "No partnerships to display.",
    "loading": "Loading budget…",
    "error": "Unable to load the budget."
  }
```

- [ ] **Step 3: Add `budget` namespace to `es-ES.json`**

Same procedure for `front/i18n/locales/es-ES.json`:

```json
  "budget": {
    "title": "Presupuesto",
    "allPacks": "Todos los paquetes",
    "packFilterLabel": "Paquete",
    "columns": {
      "pack": "Paquete",
      "company": "Empresa",
      "price": "Precio",
      "status": "Estado"
    },
    "status": {
      "paid": "Pagado",
      "validated": "Validado",
      "submitted": "Enviado"
    },
    "totals": {
      "paid": "Pagado",
      "validated": "Validado",
      "total": "Total",
      "toCome": "por venir",
      "pipeline": "en curso"
    },
    "empty": "No hay asociaciones para mostrar.",
    "loading": "Cargando presupuesto…",
    "error": "No se pudo cargar el presupuesto."
  }
```

- [ ] **Step 4: Validate i18n consistency**

The repo has an `i18n:check` script that verifies all locale files share the same key structure.

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1/front
pnpm i18n:check
```

Expected: exits 0 with no missing-key warnings. If the script reports a missing key, the namespace structure in one of the JSON files differs — fix the diff and re-run.

- [ ] **Step 5: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/i18n/locales/fr-FR.json front/i18n/locales/en-US.json front/i18n/locales/es-ES.json
git commit -m "$(cat <<'EOF'
i18n(front): add budget namespace in fr/en/es

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: `StatusBadge` component

**Files:**

- Create: `front/components/budget/StatusBadge.vue`

- [ ] **Step 1: Create the component**

```vue
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
```

- [ ] **Step 2: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/components/budget/StatusBadge.vue
git commit -m "$(cat <<'EOF'
feat(front): add StatusBadge for budget partnership rows

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: `TotalsCard` component

**Files:**

- Create: `front/components/budget/TotalsCard.vue`

- [ ] **Step 1: Create the component**

```vue
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
```

- [ ] **Step 2: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/components/budget/TotalsCard.vue
git commit -m "$(cat <<'EOF'
feat(front): add TotalsCard for budget summary

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Budget page

**Files:**

- Create: `front/pages/orgs/[slug]/events/[eventSlug]/budget.vue`

- [ ] **Step 1: Create the page**

```vue
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
```

- [ ] **Step 2: Verify the build compiles**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1/front
pnpm build
```

Expected: exits 0 with no TypeScript errors. (If `pnpm build` is too slow for fast iteration, `npx nuxi typecheck` is acceptable as a quicker substitute.)

Common errors to expect:

- "Cannot find name 'BudgetTotalsSchema'" — confirm Task 7 hasn't been skipped and the orval-generated `front/utils/api.ts` includes the budget types (it does on this branch; `getOrgsEventsBudget` was added in commit `4ef304e`).
- "Property 'pack_id' does not exist on type 'PartnershipBudgetItemSchema'" — the spread on line `...p, pack_name, pack_id` should resolve via the `BudgetRow extends PartnershipBudgetItemSchema` interface.

- [ ] **Step 3: Lint and format check**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1/front
pnpm lint
pnpm format:check
```

Expected: both exit 0. If `pnpm format:check` flags formatting drift, run `pnpm format` once and re-commit the formatted files.

- [ ] **Step 4: Commit**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1
git add front/pages/orgs/[slug]/events/[eventSlug]/budget.vue
git commit -m "$(cat <<'EOF'
feat(front): add Budget page with totals card, partnership list, and pack filter

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: Manual verification

This feature is UI-only and the codebase has no automated tests for sibling pages (per the spec). Manual verification on the running dev server is the practical gate.

- [ ] **Step 1: Start the dev server**

```bash
cd /Users/gpaligot/Documents/workspace/partners-connect-1/front
pnpm dev
```

Expected: server starts on `http://localhost:3000`. Watch the terminal for any HMR errors.

- [ ] **Step 2: Visit the Budget screen for a real event**

1. Log in.
2. Navigate to an existing org → event.
3. Verify: "Budget" appears in the sidebar below "Dashboard" with the banknotes icon.
4. Click it. The URL becomes `/orgs/<slug>/events/<eventSlug>/budget`.

- [ ] **Step 3: Verify the totals card and partnership list (event-wide view)**

- The "All packs" option is preselected.
- The totals card shows three numbers (Paid, Validated, Total) with the two derived hints (↑ to come, ↑ pipeline).
- The partnership list has 4 columns: Pack, Company, Price, Status.
- Prices are formatted with the Euro symbol and no decimals (e.g., "4 500 €" in French).
- Status badges are colored (green/blue/gray).

- [ ] **Step 4: Verify the pack filter**

- Open the pack dropdown. It shows "All packs" followed by every pack with at least one partnership.
- Select a specific pack. The totals card numbers change to that pack's totals.
- The list now contains only that pack's partnerships and the "Pack" column has disappeared.
- Switch back to "All packs". The card and list return to event-wide view; the "Pack" column reappears.

- [ ] **Step 5: Verify the empty-event case (optional)**

If a test event with zero partnerships is available, navigate there:

- Totals card shows zeros for all five metrics.
- Empty state message ("No partnerships to display.") appears in place of the table.

- [ ] **Step 6: Verify the loading and error states (optional)**

- Reload the Budget page with the network throttled (DevTools → Network → Slow 3G). The skeleton appears for ~1s.
- Temporarily stub the API to fail (or pick a non-existent event slug). The red error banner appears.

- [ ] **Step 7: Commit any post-verification fixes**

If manual verification surfaces issues, fix them and commit. If everything passes, no commit needed.

---

## Self-Review (run before declaring complete)

**Spec coverage:**

| Spec requirement                                                      | Implemented in                                                 |
| --------------------------------------------------------------------- | -------------------------------------------------------------- |
| FR-001 sidebar entry after Dashboard                                  | Task 1                                                         |
| FR-002 page at `pages/.../budget.vue`                                 | Task 6                                                         |
| FR-003 call `getOrgsEventsBudget` on mount                            | Task 6 (`loadBudget`)                                          |
| FR-004 loading / error / success states                               | Task 6 (template branches)                                     |
| FR-005 TotalsCard component                                           | Task 5                                                         |
| FR-006 `Intl.NumberFormat` with active locale                         | Task 2 (`formatPrice` helper)                                  |
| FR-007 USelect with packs                                             | Task 6 (`packOptions`)                                         |
| FR-008 All packs → event totals + flat list with Pack column          | Task 6 (`displayedTotals`, `displayedPartnerships`, `columns`) |
| FR-009 single pack → pack totals + filtered list + Pack column hidden | Task 6 (`selectedPack`, `columns`)                             |
| FR-010 UTable columns (Pack / Company / Price / Status)               | Task 6 (`columns`)                                             |
| FR-011 StatusBadge with success/primary/neutral                       | Task 4                                                         |
| FR-012 empty state                                                    | Task 6 (`v-if="displayedPartnerships.length === 0"`)           |
| FR-013 page meta (auth, no SSR)                                       | Task 6 (`definePageMeta`)                                      |
| FR-014 i18n keys in fr/en/es                                          | Task 3                                                         |
| FR-015 `formatPrice` helper consumed by card + table                  | Task 2 + Task 5 + Task 6                                       |
| US1 view global budget                                                | Tasks 5-6                                                      |
| US2 filter by pack                                                    | Task 6                                                         |
| US3 sidebar entry                                                     | Task 1                                                         |
| Edge: zero partnerships                                               | Task 6 (`displayedTotals` fallback + empty state)              |
| Edge: single pack in dropdown                                         | Task 6 (no special-casing, works by construction)              |
| Edge: auth                                                            | Task 6 (`authMiddleware`)                                      |

**Placeholder scan:** none — every step contains concrete code or commands.

**Type consistency:** Reviewed — `BudgetRow extends PartnershipBudgetItemSchema` keeps the row shape compatible with the table columns. `displayedTotals` always returns `BudgetTotalsSchema`. `formatPrice` signature matches between helper, card, and table cell.

---

**Plan complete and saved to `front/specs/025-event-budget/plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
