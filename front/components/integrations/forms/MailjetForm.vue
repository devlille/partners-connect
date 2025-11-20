<template>
  <div class="space-y-4">
    <div>
      <label for="api_key" class="block text-sm font-medium text-gray-700 mb-2">
        Clé API Mailjet <span class="text-red-500">*</span>
      </label>
      <input
        id="api_key"
        v-model="localValue.api_key"
        type="text"
        placeholder="Votre clé API Mailjet"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('api_key')"
        @input="handleInput"
      />
      <p v-if="errors.api_key" class="mt-1 text-sm text-red-600">{{ errors.api_key }}</p>
    </div>

    <div>
      <label for="api_secret" class="block text-sm font-medium text-gray-700 mb-2">
        Secret API Mailjet <span class="text-red-500">*</span>
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
      <label for="from_email" class="block text-sm font-medium text-gray-700 mb-2">
        Email de l'expéditeur <span class="text-red-500">*</span>
      </label>
      <input
        id="from_email"
        v-model="localValue.sender_email"
        type="email"
        placeholder="contact@exemple.com"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('sender_email')"
        @input="handleInput"
      />
      <p v-if="errors.sender_email" class="mt-1 text-sm text-red-600">{{ errors.sender_email }}</p>
    </div>

    <div>
      <label for="from_name" class="block text-sm font-medium text-gray-700 mb-2">
        Nom de l'expéditeur <span class="text-red-500">*</span>
      </label>
      <input
        id="from_name"
        v-model="localValue.sender_name"
        type="text"
        placeholder="Mon Événement"
        :disabled="disabled"
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        @blur="validateField('sender_name')"
        @input="handleInput"
      />
      <p v-if="errors.sender_name" class="mt-1 text-sm text-red-600">{{ errors.sender_name }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';

interface MailjetConfig {
  api_key: string;
  secret: string;
  sender_email: string;
  sender_name: string;
}

interface Props {
  modelValue: Partial<MailjetConfig>;
  disabled?: boolean;
}

interface Emits {
  (event: 'update:modelValue', value: Partial<MailjetConfig>): void;
  (event: 'update:valid', valid: boolean): void;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false
});

const emit = defineEmits<Emits>();

const localValue = reactive<Partial<MailjetConfig>>({
  api_key: props.modelValue.api_key || '',
  secret: props.modelValue.secret || '',
  sender_email: props.modelValue.sender_email || '',
  sender_name: props.modelValue.sender_name || ''
});

const errors = reactive<Partial<Record<keyof MailjetConfig, string>>>({});

function validateField(field: keyof MailjetConfig, showError = true) {
  const value = localValue[field]?.trim() || '';

  if (!value) {
    if (showError) errors[field] = 'Ce champ est obligatoire';
    return false;
  }

  // Email validation
  if (field === 'sender_email') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      if (showError) errors[field] = 'Email invalide';
      return false;
    }
  }

  if (showError) errors[field] = '';
  return true;
}

function validateAll(showErrors = false): boolean {
  const api_keyValid = validateField('api_key', showErrors);
  const secretValid = validateField('secret', showErrors);
  const sender_emailValid = validateField('sender_email', showErrors);
  const sender_nameValid = validateField('sender_name', showErrors);

  return api_keyValid && secretValid && sender_emailValid && sender_nameValid;
}

function handleInput() {
  emit('update:modelValue', { ...localValue });
  emit('update:valid', validateAll());
}

watch(() => props.modelValue, (newVal) => {
  Object.assign(localValue, newVal);
}, { deep: true });

watch(() => [localValue.api_key, localValue.secret, localValue.sender_email, localValue.sender_name], () => {
  emit('update:valid', validateAll());
}, { immediate: true });
</script>
