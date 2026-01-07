<template>
  <button
    type="button"
    class="tri-state-checkbox"
    :class="stateClass"
    :aria-checked="ariaChecked"
    role="checkbox"
    @click="cycleState"
  >
    <span class="tri-state-checkbox-icon">
      <UIcon v-if="modelValue === true" name="i-heroicons-check" class="tri-state-icon" />
      <UIcon v-else-if="modelValue === false" name="i-heroicons-x-mark" class="tri-state-icon" />
      <span v-else class="tri-state-icon" />
    </span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  modelValue: boolean | null;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:modelValue': [value: boolean | null];
}>();

const stateClass = computed(() => {
  if (props.modelValue === true) return 'tri-state-checked';
  if (props.modelValue === false) return 'tri-state-unchecked';
  return 'tri-state-indeterminate';
});

const ariaChecked = computed(() => {
  if (props.modelValue === true) return 'true';
  if (props.modelValue === false) return 'false';
  return 'mixed';
});

/**
 * Cycle through states: null -> true -> false -> null
 */
function cycleState() {
  if (props.modelValue === null) {
    emit('update:modelValue', true);
  } else if (props.modelValue === true) {
    emit('update:modelValue', false);
  } else {
    emit('update:modelValue', null);
  }
}
</script>

<style scoped>
.tri-state-checkbox {
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 0.25rem;
  border-width: 2px;
  border-style: solid;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.tri-state-checkbox:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--color-primary-500, #3b82f6);
}

.tri-state-indeterminate {
  border-color: #d1d5db;
  background-color: #f3f4f6;
  color: #9ca3af;
}

.tri-state-checked {
  border-color: #22c55e;
  background-color: #22c55e;
  color: white;
}

.tri-state-unchecked {
  border-color: #ef4444;
  background-color: #ef4444;
  color: white;
}

.tri-state-checkbox-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tri-state-icon {
  width: 0.75rem;
  height: 0.75rem;
}
</style>
