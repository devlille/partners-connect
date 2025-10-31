<template>
  <section
    role="region"
    :aria-label="$t('sponsors.filters.filterPanel')"
    class="filter-panel bg-white rounded-lg border border-gray-200 p-4 mb-6"
  >
    <!-- Header with Toggle for Mobile -->
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <!-- Mobile: Collapsible toggle button -->
        <button
          v-if="collapsible"
          @click="toggleFilters"
          class="md:hidden p-2 hover:bg-gray-100 rounded-lg transition-colors touch-target"
          :aria-expanded="isExpanded"
          :aria-controls="'filter-content'"
          :aria-label="isExpanded ? 'Masquer les filtres' : 'Afficher les filtres'"
        >
          <UIcon
            :name="isExpanded ? 'i-heroicons-chevron-up' : 'i-heroicons-chevron-down'"
            class="w-5 h-5"
          />
        </button>
        <h2 class="text-lg font-semibold text-gray-900">
          {{ $t('sponsors.filters.filterPanel') }}
          <span v-if="activeFilterCount > 0" class="ml-2 text-sm font-normal text-gray-500">
            ({{ activeFilterCount }})
          </span>
        </h2>
      </div>
      <UButton
        v-if="activeFilterCount > 0"
        :label="$t('sponsors.filters.clearAll')"
        size="sm"
        color="gray"
        variant="ghost"
        icon="i-heroicons-x-mark"
        @click="$emit('clear-all')"
        :aria-label="$t('sponsors.filters.clearAll')"
        class="touch-target min-h-[44px]"
      />
    </div>

    <!-- Collapsible Filter Content (expanded on desktop, toggleable on mobile) -->
    <div
      :id="'filter-content'"
      :class="[
        'flex flex-col gap-6',
        'transition-all duration-300 ease-in-out',
        collapsible && !isExpanded ? 'hidden md:flex' : 'flex'
      ]"
    >
      <!-- Pack Filter (full width on top) -->
      <div class="w-full md:w-1/3">
        <PackFilter
          :model-value="modelValue.packId"
          :packs="packs"
          @update:model-value="updateFilter('packId', $event)"
        />
      </div>

      <!-- Status Filters (below pack filter) -->
      <div class="w-full">
        <StatusFilters
          :model-value="{
            validated: modelValue.validated,
            paid: modelValue.paid,
            agreementGenerated: modelValue.agreementGenerated,
            agreementSigned: modelValue.agreementSigned,
            suggestion: modelValue.suggestion
          }"
          @update:model-value="updateStatusFilters($event)"
        />
      </div>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="mt-4 flex items-center gap-2 text-sm text-gray-500">
      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
      <span>{{ $t('common.loading') }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { FilterState } from '~/types/sponsors'
import type { SponsoringPack } from '~/utils/api'
import PackFilter from '~/components/sponsors/PackFilter.vue'
import StatusFilters from '~/components/sponsors/StatusFilters.vue'

interface FilterPanelProps {
  /** Current filter state (v-model) */
  modelValue: FilterState
  /** List of packs for dropdown */
  packs: SponsoringPack[]
  /** Whether filters are being applied (loading state) */
  loading?: boolean
  /** Number of active filters */
  activeFilterCount?: number
  /** Whether the filter panel is collapsible on mobile */
  collapsible?: boolean
}

interface FilterPanelEmits {
  (e: 'update:modelValue', value: FilterState): void
  (e: 'clear-all'): void
}

const props = withDefaults(defineProps<FilterPanelProps>(), {
  loading: false,
  activeFilterCount: 0,
  collapsible: true
})

const emit = defineEmits<FilterPanelEmits>()

// Mobile collapsible state
const isExpanded = ref(false)

function toggleFilters() {
  isExpanded.value = !isExpanded.value
}

/**
 * Update a specific filter and emit the full state
 */
function updateFilter(key: keyof FilterState, value: any) {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value
  })
}

/**
 * Update status filters when StatusFilters component emits changes
 */
function updateStatusFilters(statusUpdates: Partial<{
  validated: boolean | null
  paid: boolean | null
  agreementGenerated: boolean | null
  agreementSigned: boolean | null
  suggestion: boolean | null
}>) {
  emit('update:modelValue', {
    ...props.modelValue,
    ...statusUpdates
  })
}
</script>

<style scoped>
/* Touch target optimization for mobile (min 44x44px per WCAG guidelines) */
.touch-target {
  min-height: 44px;
  min-width: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* Mobile optimizations */
@media (max-width: 767px) {
  .filter-panel {
    padding: 1rem;
  }

  /* Ensure filter badges wrap properly */
  .active-filters {
    flex-wrap: wrap;
  }

  /* Make dropdowns full width on mobile */
  :deep(.pack-filter) {
    width: 100%;
  }
}

/* Smooth transitions for collapsible behavior */
@media (prefers-reduced-motion: no-preference) {
  .transition-all {
    transition-property: all;
    transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
    transition-duration: 300ms;
  }
}

/* Respect reduced motion preference */
@media (prefers-reduced-motion: reduce) {
  .transition-all {
    transition: none;
  }
}
</style>
