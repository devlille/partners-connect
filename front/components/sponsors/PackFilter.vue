<template>
  <div class="pack-filter">
    <label class="block text-sm font-medium text-gray-700 mb-1">
      {{ $t('sponsors.filters.pack') }}
    </label>
    <select
      :value="modelValue || ''"
      @change="handleChange"
      class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
    >
      <option value="">{{ placeholder || $t('sponsors.filters.allPacks') }}</option>
      <option v-for="pack in packs" :key="pack.id" :value="pack.id">
        {{ pack.name }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
import type { SponsoringPack } from '~/utils/api'

interface PackFilterProps {
  /** Currently selected pack ID (v-model) */
  modelValue: string | null
  /** List of available packs to choose from */
  packs: SponsoringPack[]
  /** Placeholder text when no pack selected */
  placeholder?: string
}

interface PackFilterEmits {
  (e: 'update:modelValue', value: string | null): void
}

const props = defineProps<PackFilterProps>()
const emit = defineEmits<PackFilterEmits>()

function handleChange(event: Event) {
  const target = event.target as HTMLSelectElement
  const value = target.value === '' || target.value === 'null' ? null : target.value
  emit('update:modelValue', value)
}
</script>
