<template>
  <div class="space-y-4">
    <div>
      <label for="api_key" class="block text-sm font-medium text-gray-700 mb-2">
        Clé API Qonto <span class="text-red-500">*</span>
      </label>
      <input
        id="api_key"
        v-model="localValue.api_key"
        type="text"
        placeholder="Votre clé API Qonto"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('api_key')"
        @input="handleInput"
      />
      <p v-if="errors.api_key" class="mt-1 text-sm text-red-600">{{ errors.api_key }}</p>
    </div>

    <div>
      <label for="api_secret" class="block text-sm font-medium text-gray-700 mb-2">
        Secret API Qonto <span class="text-red-500">*</span>
      </label>
      <input
        id="api_secret"
        v-model="localValue.secret"
        type="password"
        placeholder="Votre secret API"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('secret')"
        @input="handleInput"
      />
      <p v-if="errors.secret" class="mt-1 text-sm text-red-600">{{ errors.secret }}</p>
    </div>

    <div>
      <label for="sandbox_token" class="block text-sm font-medium text-gray-700 mb-2">
        Token Sandbox <span class="text-red-500">*</span>
      </label>
      <input
        id="sandbox_token"
        v-model="localValue.sandbox_token"
        type="text"
        placeholder="Votre token sandbox Qonto"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('sandbox_token')"
        @input="handleInput"
      />
      <p v-if="errors.sandbox_token" class="mt-1 text-sm text-red-600">{{ errors.sandbox_token }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
interface QontoConfig {
  api_key: string;
  secret: string;
  sandbox_token: string;
}

interface Props {
  modelValue: Partial<QontoConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<QontoConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

// Utilise le composable pour la validation
const { localValue, errors, handleInput } = useIntegrationFormValidation<Partial<QontoConfig>>(
  props,
  emit,
  ['api_key', 'secret', 'sandbox_token']
);

// Initialiser les valeurs par défaut
Object.assign(localValue, {
  api_key: props.modelValue.api_key || '',
  secret: props.modelValue.secret || '',
  sandbox_token: props.modelValue.sandbox_token || ''
});
</script>
