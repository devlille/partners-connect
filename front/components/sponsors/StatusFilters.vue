<template>
  <fieldset class="status-filters">
    <legend class="text-sm font-medium text-gray-900 mb-3">
      {{ $t('sponsors.filters.statusFilters') }}
    </legend>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <!-- Validated Filter -->
      <div class="flex items-center gap-2">
        <UCheckbox
          v-model="internalValidated"
          :label="$t('sponsors.filters.validated')"
        />
      </div>

      <!-- Paid Filter -->
      <div class="flex items-center gap-2">
        <UCheckbox
          v-model="internalPaid"
          :label="$t('sponsors.filters.paid')"
        />
      </div>

      <!-- Agreement Generated Filter -->
      <div class="flex items-center gap-2">
        <UCheckbox
          v-model="internalAgreementGenerated"
          :label="$t('sponsors.filters.agreementGenerated')"
        />
      </div>

      <!-- Agreement Signed Filter -->
      <div class="flex items-center gap-2">
        <UCheckbox
          v-model="internalAgreementSigned"
          :label="$t('sponsors.filters.agreementSigned')"
        />
      </div>

      <!-- Suggestion Filter -->
      <div class="flex items-center gap-2">
        <UCheckbox
          v-model="internalSuggestion"
          :label="$t('sponsors.filters.suggestion')"
        />
      </div>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
interface StatusFiltersProps {
  /** Current status filter values (v-model) */
  modelValue: {
    validated: boolean | null
    paid: boolean | null
    agreementGenerated: boolean | null
    agreementSigned: boolean | null
    suggestion: boolean | null
  }
}

interface StatusFiltersEmits {
  (e: 'update:modelValue', value: Partial<StatusFiltersProps['modelValue']>): void
}

const props = defineProps<StatusFiltersProps>()
const emit = defineEmits<StatusFiltersEmits>()

// Internal refs that convert null to false for checkboxes
const internalValidated = computed({
  get: () => props.modelValue.validated === true,
  set: (value: boolean) => emit('update:modelValue', { ...props.modelValue, validated: value ? true : null })
})

const internalPaid = computed({
  get: () => props.modelValue.paid === true,
  set: (value: boolean) => emit('update:modelValue', { ...props.modelValue, paid: value ? true : null })
})

const internalAgreementGenerated = computed({
  get: () => props.modelValue.agreementGenerated === true,
  set: (value: boolean) => emit('update:modelValue', { ...props.modelValue, agreementGenerated: value ? true : null })
})

const internalAgreementSigned = computed({
  get: () => props.modelValue.agreementSigned === true,
  set: (value: boolean) => emit('update:modelValue', { ...props.modelValue, agreementSigned: value ? true : null })
})

const internalSuggestion = computed({
  get: () => props.modelValue.suggestion === true,
  set: (value: boolean) => emit('update:modelValue', { ...props.modelValue, suggestion: value ? true : null })
})
</script>

<style scoped>
.status-filters fieldset {
  border: none;
  padding: 0;
  margin: 0;
}

.status-filters legend {
  padding: 0;
}
</style>
