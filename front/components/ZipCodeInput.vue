<template>
  <div>
    <label v-if="label" :for="inputId" class="block text-sm font-medium text-gray-700 mb-2">
      {{ label }}
    </label>
    <UInput
      :id="inputId"
      :model-value="modelValue"
      type="text"
      inputmode="numeric"
      maxlength="5"
      :placeholder="placeholder"
      :disabled="disabled"
      :class="[inputClass, { 'border-red-500 focus:border-red-500 focus:ring-red-500': showError && errorMessage }]"
      @input="handleInput"
      @blur="handleBlur"
    />
    <p v-if="showError && errorMessage" class="mt-1 text-sm text-red-600">
      {{ errorMessage }}
    </p>
    <p v-else-if="hint" class="mt-1 text-sm text-gray-500">
      {{ hint }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { ZIP_CODE_REGEX, VALIDATION_MESSAGES } from '~/constants/validation';

interface Props {
  modelValue: string | null | undefined;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  hint?: string;
  inputClass?: string;
  validate?: boolean;
  showErrorOnInput?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  label: 'Code postal',
  placeholder: '00000',
  disabled: false,
  required: false,
  validate: true,
  inputClass: 'w-full',
  showErrorOnInput: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string | null];
  'validation': [isValid: boolean];
}>();

// Générer un ID unique pour le champ
const inputId = computed(() => `zip-code-${Math.random().toString(36).substring(2, 9)}`);

const internalValue = ref(props.modelValue || '');
const showError = ref(false);

const errorMessage = computed(() => {
  if (!props.validate) return null;

  const value = internalValue.value.trim();

  if (props.required && !value) {
    return VALIDATION_MESSAGES.REQUIRED;
  }

  if (value && !ZIP_CODE_REGEX.test(value)) {
    return VALIDATION_MESSAGES.ZIP_CODE_INVALID;
  }

  return null;
});

const isValid = computed(() => {
  if (!props.validate) return true;

  const value = internalValue.value.trim();

  if (props.required && !value) return false;
  if (value && !ZIP_CODE_REGEX.test(value)) return false;

  return true;
});

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement;
  let value = target.value;

  // Ne garder que les chiffres
  value = value.replace(/\D/g, '');

  // Limiter à 5 chiffres
  value = value.substring(0, 5);

  internalValue.value = value;
  emit('update:modelValue', value || null);
  emit('validation', isValid.value);

  // Afficher l'erreur en temps réel si l'option est activée
  if (props.showErrorOnInput) {
    showError.value = value.length > 0;
  } else {
    // Masquer l'erreur lors de la saisie par défaut
    showError.value = false;
  }
}

function handleBlur() {
  // Afficher l'erreur uniquement après que l'utilisateur ait quitté le champ
  if (props.validate) {
    showError.value = true;
    emit('validation', isValid.value);
  }
}

// Synchroniser avec la prop modelValue
watch(() => props.modelValue, (newValue) => {
  internalValue.value = newValue || '';
});

// Émettre l'état de validation initial
onMounted(() => {
  if (props.validate) {
    emit('validation', isValid.value);
  }
});
</script>
