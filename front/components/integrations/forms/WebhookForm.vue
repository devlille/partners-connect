<template>
  <div class="space-y-4">
    <div>
      <label for="url" class="block text-sm font-medium text-gray-700 mb-2">
        URL du webhook <span class="text-red-500">*</span>
      </label>
      <input
        id="url"
        v-model="localValue.url"
        type="url"
        placeholder="https://exemple.com/webhook"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('url')"
        @input="handleInput"
      />
      <p v-if="errors.url" class="mt-1 text-sm text-red-600">{{ errors.url }}</p>
    </div>

    <div>
      <label for="secret" class="block text-sm font-medium text-gray-700 mb-2">
        Secret HMAC <span class="text-red-500">*</span>
      </label>
      <input
        id="secret"
        v-model="localValue.secret"
        type="password"
        placeholder="Minimum 16 caractères"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('secret')"
        @input="handleInput"
      />
      <p v-if="errors.secret" class="mt-1 text-sm text-red-600">{{ errors.secret }}</p>
    </div>

    <div>
      <label for="type" class="block text-sm font-medium text-gray-700 mb-2">
        Type de webhook <span class="text-red-500">*</span>
      </label>
      <select
        id="type"
        v-model="localValue.type"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('type')"
        @change="handleInput"
      >
        <option value="">Sélectionnez un type</option>
        <option v-for="type in availableTypes" :key="type.value" :value="type.value">
          {{ type.label }}
        </option>
      </select>
      <p v-if="errors.type" class="mt-1 text-sm text-red-600">{{ errors.type }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';

interface WebhookConfig {
  url: string;
  secret: string;
  type: string;
}

interface Props {
  modelValue: Partial<WebhookConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<WebhookConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

const localValue = reactive<Partial<WebhookConfig>>({
  url: props.modelValue.url || '',
  secret: props.modelValue.secret || '',
  type: props.modelValue.type || ''
});

const errors = reactive<Partial<Record<keyof WebhookConfig, string>>>({});

const availableTypes = [
  { value: 'ALL', label: 'Tous les événements' },
  { value: 'PARTNERSHIP', label: 'Partenariats uniquement' }
];

function validateField(field: keyof WebhookConfig, showError = true) {
  if (field === 'url') {
    const value = localValue.url?.trim() || '';
    if (!value) {
      if (showError) errors.url = 'Ce champ est obligatoire';
      return false;
    }
    try {
      const url = new URL(value);
      if (!url.protocol.startsWith('https')) {
        if (showError) errors.url = 'L\'URL doit utiliser HTTPS';
        return false;
      }
    } catch {
      if (showError) errors.url = 'URL invalide';
      return false;
    }
    if (showError) errors.url = '';
    return true;
  }

  if (field === 'secret') {
    const value = localValue.secret?.trim() || '';
    if (!value) {
      if (showError) errors.secret = 'Ce champ est obligatoire';
      return false;
    }
    if (value.length < 16) {
      if (showError) errors.secret = 'Le secret doit contenir au moins 16 caractères';
      return false;
    }
    if (showError) errors.secret = '';
    return true;
  }

  if (field === 'type') {
    const value = localValue.type?.trim() || '';
    if (!value) {
      if (showError) errors.type = 'Ce champ est obligatoire';
      return false;
    }
    if (showError) errors.type = '';
    return true;
  }

  return true;
}

function validateAll(showErrors = false): boolean {
  const urlValid = validateField('url', showErrors);
  const secretValid = validateField('secret', showErrors);
  const typeValid = validateField('type', showErrors);

  return urlValid && secretValid && typeValid;
}

function handleInput() {
  emit('update:modelValue', { ...localValue });
  emit('update:valid', validateAll());
}

watch(() => props.modelValue, (newVal) => {
  Object.assign(localValue, newVal);
}, { deep: true });

watch(() => [localValue.url, localValue.secret, localValue.type], () => {
  emit('update:valid', validateAll());
}, { immediate: true });
</script>
