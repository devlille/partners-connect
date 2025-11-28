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

// Validation personnalisée pour le channel (doit commencer par #)
const customValidators = {
  channel: (value: string) => {
    if (value && !value.startsWith('#')) {
      return 'Le canal doit commencer par #';
    }
    return null;
  }
};

// Utilise le composable pour la validation
const { localValue, errors, validateField, handleInput } = useIntegrationFormValidation<Partial<SlackConfig>>(
  props,
  emit,
  ['token', 'channel'],
  customValidators
);

// Initialiser les valeurs par défaut
Object.assign(localValue, {
  token: props.modelValue.token || '',
  channel: props.modelValue.channel || ''
});

// Auto-correction du canal pour ajouter # si manquant
watch(() => localValue.channel, (newValue) => {
  if (newValue && !newValue.startsWith('#')) {
    localValue.channel = '#' + newValue;
  }
});
</script>
