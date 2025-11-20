<template>
  <div class="space-y-4">
    <div>
      <label for="api_key" class="block text-sm font-medium text-gray-700 mb-2">
        Clé API OpenPlanner <span class="text-red-500">*</span>
      </label>
      <input
        id="api_key"
        v-model="localValue.api_key"
        type="text"
        placeholder="Votre clé API OpenPlanner"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('api_key')"
        @input="handleInput"
      />
      <p v-if="errors.api_key" class="mt-1 text-sm text-red-600">{{ errors.api_key }}</p>
    </div>

    <div>
      <label for="event_id" class="block text-sm font-medium text-gray-700 mb-2">
        ID de l'événement <span class="text-red-500">*</span>
      </label>
      <input
        id="event_id"
        v-model="localValue.event_id"
        type="text"
        placeholder="event-123"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('event_id')"
        @input="handleInput"
      />
      <p v-if="errors.event_id" class="mt-1 text-sm text-red-600">{{ errors.event_id }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';

interface OpenPlannerConfig {
  api_key: string;
  event_id: string;
}

interface Props {
  modelValue: Partial<OpenPlannerConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<OpenPlannerConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

const localValue = reactive<Partial<OpenPlannerConfig>>({
  api_key: props.modelValue.api_key || '',
  event_id: props.modelValue.event_id || ''
});

const errors = reactive<Partial<Record<keyof OpenPlannerConfig, string>>>({});

function validateField(field: keyof OpenPlannerConfig, showError = true) {
  const value = localValue[field]?.trim() || '';

  if (!value) {
    if (showError) errors[field] = 'Ce champ est obligatoire';
    return false;
  }

  if (showError) errors[field] = '';
  return true;
}

function validateAll(showErrors = false): boolean {
  const api_keyValid = validateField('api_key', showErrors);
  const event_idValid = validateField('event_id', showErrors);

  return api_keyValid && event_idValid;
}

function handleInput() {
  emit('update:modelValue', { ...localValue });
  emit('update:valid', validateAll());
}

watch(() => props.modelValue, (newVal) => {
  Object.assign(localValue, newVal);
}, { deep: true });

watch(() => [localValue.api_key, localValue.event_id], () => {
  emit('update:valid', validateAll());
}, { immediate: true });
</script>
