<template>
  <div class="space-y-4">
    <div>
      <label for="token" class="block text-sm font-medium text-gray-700 mb-2">
        Token Slack <span class="text-red-500">*</span>
      </label>
      <input
        id="token"
        v-model="localValue.token"
        type="text"
        placeholder="xoxb-..."
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('token')"
        @input="handleInput"
      />
      <p v-if="errors.token" class="mt-1 text-sm text-red-600">{{ errors.token }}</p>
    </div>

    <div>
      <label for="channel" class="block text-sm font-medium text-gray-700 mb-2">
        Canal <span class="text-red-500">*</span>
      </label>
      <input
        id="channel"
        v-model="localValue.channel"
        type="text"
        placeholder="#general"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('channel')"
        @input="handleInput"
      />
      <p v-if="errors.channel" class="mt-1 text-sm text-red-600">{{ errors.channel }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';

interface SlackConfig {
  token: string;
  channel: string;
}

interface Props {
  modelValue: Partial<SlackConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<SlackConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

const localValue = reactive<Partial<SlackConfig>>({
  token: props.modelValue.token || '',
  channel: props.modelValue.channel || ''
});

const errors = reactive<Partial<Record<keyof SlackConfig, string>>>({});

function validateField(field: keyof SlackConfig, showError = true) {
  const value = localValue[field]?.trim() || '';

  if (!value) {
    if (showError) errors[field] = 'Ce champ est obligatoire';
    return false;
  }

  // Channel validation (ensure it starts with #)
  if (field === 'channel') {
    if (!value.startsWith('#')) {
      localValue.channel = '#' + value;
    }
  }

  if (showError) errors[field] = '';
  return true;
}

function validateAll(showErrors = false): boolean {
  const tokenValid = validateField('token', showErrors);
  const channelValid = validateField('channel', showErrors);

  return tokenValid && channelValid;
}

function handleInput() {
  emit('update:modelValue', { ...localValue });
  emit('update:valid', validateAll());
}

watch(() => props.modelValue, (newVal) => {
  Object.assign(localValue, newVal);
}, { deep: true });

watch(() => [localValue.token, localValue.channel], () => {
  emit('update:valid', validateAll());
}, { immediate: true });
</script>
