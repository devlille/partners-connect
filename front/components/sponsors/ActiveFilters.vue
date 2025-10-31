<template>
  <div v-if="hasActiveFilters" class="active-filters flex flex-wrap items-center gap-2 mb-4">
    <span class="text-sm font-medium text-gray-700">{{ $t('common.activeFilters') || 'Filtres actifs:' }}</span>

    <!-- Pack Filter Badge -->
    <UBadge
      v-if="filters.packId"
      color="primary"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.pack') }}: {{ getPackName(filters.packId) }}</span>
      <button
        @click="$emit('clear', 'packId')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.pack')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Validated Badge -->
    <UBadge
      v-if="filters.validated !== null"
      color="blue"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.validated') }}: {{ filters.validated ? $t('common.yes') : $t('common.no') }}</span>
      <button
        @click="$emit('clear', 'validated')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.validated')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Paid Badge -->
    <UBadge
      v-if="filters.paid !== null"
      color="green"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.paid') }}: {{ filters.paid ? $t('common.yes') : $t('common.no') }}</span>
      <button
        @click="$emit('clear', 'paid')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.paid')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Agreement Generated Badge -->
    <UBadge
      v-if="filters.agreementGenerated !== null"
      color="purple"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.agreementGenerated') }}: {{ filters.agreementGenerated ? $t('common.yes') : $t('common.no') }}</span>
      <button
        @click="$emit('clear', 'agreementGenerated')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.agreementGenerated')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Agreement Signed Badge -->
    <UBadge
      v-if="filters.agreementSigned !== null"
      color="orange"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.agreementSigned') }}: {{ filters.agreementSigned ? $t('common.yes') : $t('common.no') }}</span>
      <button
        @click="$emit('clear', 'agreementSigned')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.agreementSigned')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Suggestion Badge -->
    <UBadge
      v-if="filters.suggestion !== null"
      color="yellow"
      variant="subtle"
      class="flex items-center gap-1"
    >
      <span>{{ $t('sponsors.filters.suggestion') }}: {{ filters.suggestion ? $t('common.yes') : $t('common.no') }}</span>
      <button
        @click="$emit('clear', 'suggestion')"
        class="ml-1 hover:text-red-600"
        :aria-label="`${$t('common.clear')} ${$t('sponsors.filters.suggestion')}`"
      >
        <UIcon name="i-heroicons-x-mark" class="w-4 h-4" />
      </button>
    </UBadge>

    <!-- Clear All Button -->
    <UButton
      size="xs"
      color="gray"
      variant="ghost"
      @click="$emit('clear-all')"
      :aria-label="$t('sponsors.filters.clearAll')"
    >
      {{ $t('sponsors.filters.clearAll') }}
    </UButton>
  </div>
</template>

<script setup lang="ts">
import type { FilterState } from '~/types/sponsors'
import type { SponsoringPack } from '~/utils/api'

interface ActiveFiltersProps {
  /** Current filter state */
  filters: FilterState
  /** List of packs (to display pack name instead of ID) */
  packs: SponsoringPack[]
}

interface ActiveFiltersEmits {
  /** Emitted when user clicks X on a specific filter badge */
  (e: 'clear', key: keyof FilterState): void
  /** Emitted when user clicks "Clear All" */
  (e: 'clear-all'): void
}

const props = defineProps<ActiveFiltersProps>()
const emit = defineEmits<ActiveFiltersEmits>()

/**
 * Check if there are any active filters
 */
const hasActiveFilters = computed(() => {
  return Object.values(props.filters).some(value => value !== null)
})

/**
 * Get pack name from pack ID
 */
function getPackName(packId: string): string {
  const pack = props.packs.find(p => p.id === packId)
  return pack?.name || packId
}
</script>
