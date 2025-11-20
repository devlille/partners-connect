<template>
  <div class="space-y-4">
    <div>
      <label for="basic" class="block text-sm font-medium text-gray-700 mb-2">
        Basic Auth <span class="text-red-500">*</span>
      </label>
      <input
        id="basic"
        v-model="localValue.basic"
        type="text"
        placeholder="Votre identifiant Basic Auth"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('basic')"
        @input="handleInput"
      />
      <p v-if="errors.basic" class="mt-1 text-sm text-red-600">{{ errors.basic }}</p>
    </div>

    <div>
      <label for="event_id" class="block text-sm font-medium text-gray-700 mb-2">
        ID de l'événement <span class="text-red-500">*</span>
      </label>
      <input
        id="event_id"
        v-model="localValue.event_id"
        type="text"
        placeholder="12345"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('event_id')"
        @input="handleInput"
      />
      <p v-if="errors.event_id" class="mt-1 text-sm text-red-600">{{ errors.event_id }}</p>
    </div>

    <div>
      <label for="rate_id" class="block text-sm font-medium text-gray-700 mb-2">
        ID du tarif <span class="text-red-500">*</span>
      </label>
      <input
        id="rate_id"
        v-model="localValue.rate_id"
        type="text"
        placeholder="67890"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('rate_id')"
        @input="handleInput"
      />
      <p v-if="errors.rate_id" class="mt-1 text-sm text-red-600">{{ errors.rate_id }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';

interface BilletwebConfig {
  basic: string;
  event_id: string;
  rate_id: string;
}

interface Props {
  modelValue: Partial<BilletwebConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<BilletwebConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

const localValue = reactive<Partial<BilletwebConfig>>({
  basic: props.modelValue.basic || '',
  event_id: props.modelValue.event_id || '',
  rate_id: props.modelValue.rate_id || ''
});

const errors = reactive<Partial<Record<keyof BilletwebConfig, string>>>({});

function validateField(field: keyof BilletwebConfig, showError = true) {
  const value = localValue[field]?.trim() || '';

  if (!value) {
    if (showError) errors[field] = 'Ce champ est obligatoire';
    return false;
  }

  if (showError) errors[field] = '';
  return true;
}

function validateAll(showErrors = false): boolean {
  const basicValid = validateField('basic', showErrors);
  const event_idValid = validateField('event_id', showErrors);
  const rate_idValid = validateField('rate_id', showErrors);

  return basicValid && event_idValid && rate_idValid;
}

function handleInput() {
  emit('update:modelValue', { ...localValue });
  emit('update:valid', validateAll());
}

watch(() => props.modelValue, (newVal) => {
  Object.assign(localValue, newVal);
}, { deep: true });

watch(() => [localValue.basic, localValue.event_id, localValue.rate_id], () => {
  emit('update:valid', validateAll());
}, { immediate: true });
</script>
