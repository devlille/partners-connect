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
        color="neutral"
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
      <!-- Dynamic Filters based on metadata -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <!-- Pack Filter (always first if available) -->
        <div v-if="hasPackFilter" class="w-full">
          <PackFilter
            :model-value="modelValue.packId"
            :packs="packs"
            @update:model-value="updateFilter('packId', $event)"
          />
        </div>

        <!-- String filters with values (dropdowns) -->
        <div v-for="filter in stringFiltersWithValues" :key="filter.name" class="w-full">
          <label
            :for="`filter-${filter.name}`"
            class="block text-sm font-medium text-gray-700 mb-2"
          >
            {{ getFilterLabel(filter.name) }}
          </label>
          <select
            :id="`filter-${filter.name}`"
            :value="getFilterValue(filter.name)"
            @change="updateFilterFromEvent(filter.name, $event)"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          >
            <option value="">{{ $t('sponsors.filters.all') }}</option>
            <option v-for="option in filter.values" :key="option.value" :value="option.value">
              {{ option.display_value }}
            </option>
          </select>
        </div>
      </div>

      <!-- Boolean Filters (checkboxes) -->
      <fieldset v-if="booleanFilters.length > 0" class="status-filters">
        <legend class="text-sm font-medium text-gray-900 mb-3">
          {{ $t('sponsors.filters.statusFilters') }}
        </legend>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div v-for="filter in booleanFilters" :key="filter.name" class="flex items-center gap-2">
            <UCheckbox
              :model-value="getFilterValue(filter.name) === true"
              :label="getFilterLabel(filter.name)"
              @update:model-value="updateBooleanFilter(filter.name, $event)"
            />
          </div>
        </div>
      </fieldset>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="mt-4 flex items-center gap-2 text-sm text-gray-500">
      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-900"></div>
      <span>{{ $t('common.loading') }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { FilterState, FilterMetadata, PartnershipsMetadata } from '~/types/sponsors'
import type { SponsoringPack } from '~/utils/api'
import PackFilter from '~/components/sponsors/PackFilter.vue'

interface FilterPanelProps {
  /** Current filter state (v-model) */
  modelValue: FilterState
  /** List of packs for dropdown */
  packs: SponsoringPack[]
  /** Metadata from API containing available filters */
  metadata?: PartnershipsMetadata | null
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
  collapsible: true,
  metadata: null
})

const emit = defineEmits<FilterPanelEmits>()
const { t } = useI18n()

// Mobile collapsible state
const isExpanded = ref(false)

// Mapping from API filter names to FilterState keys
const filterNameMapping: Record<string, keyof FilterState> = {
  'pack_id': 'packId',
  'validated': 'validated',
  'paid': 'paid',
  'suggestion': 'suggestion',
  'agreement-generated': 'agreementGenerated',
  'agreement-signed': 'agreementSigned',
  'organiser': 'organiser',
}

// Mapping from API filter names to i18n keys
const filterLabelMapping: Record<string, string> = {
  'pack_id': 'sponsors.filters.pack',
  'validated': 'sponsors.filters.validated',
  'paid': 'sponsors.filters.paid',
  'suggestion': 'sponsors.filters.suggestion',
  'agreement-generated': 'sponsors.filters.agreementGenerated',
  'agreement-signed': 'sponsors.filters.agreementSigned',
  'organiser': 'sponsors.filters.organiser',
}

// Check if pack filter is available in metadata
const hasPackFilter = computed(() => {
  if (!props.metadata?.filters) return true // Default to showing pack filter
  return props.metadata.filters.some(f => f.name === 'pack_id')
})

// Get boolean filters from metadata
const booleanFilters = computed<FilterMetadata[]>(() => {
  if (!props.metadata?.filters) {
    // Default filters when no metadata
    return [
      { name: 'validated', type: 'boolean' },
      { name: 'paid', type: 'boolean' },
      { name: 'agreement-generated', type: 'boolean' },
      { name: 'agreement-signed', type: 'boolean' },
      { name: 'suggestion', type: 'boolean' },
    ]
  }
  return props.metadata.filters.filter(f => f.type === 'boolean')
})

// Get string filters with values (for dropdowns, excluding pack_id which has its own component)
const stringFiltersWithValues = computed<FilterMetadata[]>(() => {
  if (!props.metadata?.filters) return []
  return props.metadata.filters.filter(f => f.type === 'string' && f.values && f.name !== 'pack_id')
})

function toggleFilters() {
  isExpanded.value = !isExpanded.value
}

function getFilterLabel(filterName: string): string {
  const key = filterLabelMapping[filterName]
  if (key) {
    return t(key)
  }
  // Fallback: capitalize the filter name
  return filterName.charAt(0).toUpperCase() + filterName.slice(1).replace(/-/g, ' ')
}

function getFilterStateKey(filterName: string): keyof FilterState {
  return filterNameMapping[filterName] || (filterName as keyof FilterState)
}

function getFilterValue(filterName: string): any {
  const key = getFilterStateKey(filterName)
  return props.modelValue[key]
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

function updateFilterFromEvent(filterName: string, event: Event) {
  const target = event.target as HTMLSelectElement
  const value = target.value || null
  const key = getFilterStateKey(filterName)
  updateFilter(key, value)
}

function updateBooleanFilter(filterName: string, value: boolean) {
  const key = getFilterStateKey(filterName)
  // Always send true or false, never null
  updateFilter(key, value)
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

.status-filters fieldset {
  border: none;
  padding: 0;
  margin: 0;
}

.status-filters legend {
  padding: 0;
}
</style>
