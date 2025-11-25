<template>
  <div
    v-if="modelValue"
    :class="alertClasses"
    role="alert"
  >
    <div class="flex items-start">
      <div v-if="showIcon" class="flex-shrink-0">
        <UIcon :name="iconName" :class="iconClasses" />
      </div>
      <div :class="{ 'ml-3': showIcon }">
        <h3 v-if="title" :class="titleClasses">
          {{ title }}
        </h3>
        <div :class="messageClasses">
          <slot>{{ message }}</slot>
        </div>
      </div>
      <div v-if="dismissible" class="ml-auto pl-3">
        <button
          type="button"
          :class="closeButtonClasses"
          @click="handleDismiss"
          aria-label="Fermer"
        >
          <UIcon name="i-heroicons-x-mark" class="w-5 h-5" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
type AlertType = 'error' | 'success' | 'warning' | 'info';

interface Props {
  type?: AlertType;
  title?: string;
  message?: string;
  dismissible?: boolean;
  showIcon?: boolean;
  modelValue?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'error',
  dismissible: false,
  showIcon: true,
  modelValue: true,
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'dismiss': [];
}>();

const handleDismiss = () => {
  emit('update:modelValue', false);
  emit('dismiss');
};

const alertConfig = computed(() => {
  const configs = {
    error: {
      container: 'bg-red-50 border border-red-200 text-red-700',
      icon: 'i-heroicons-exclamation-circle',
      iconColor: 'text-red-400',
      title: 'text-red-800 font-medium',
      message: 'text-red-700',
      closeButton: 'text-red-400 hover:text-red-600',
    },
    success: {
      container: 'bg-green-50 border border-green-200 text-green-700',
      icon: 'i-heroicons-check-circle',
      iconColor: 'text-green-400',
      title: 'text-green-800 font-medium',
      message: 'text-green-700',
      closeButton: 'text-green-400 hover:text-green-600',
    },
    warning: {
      container: 'bg-yellow-50 border border-yellow-200 text-yellow-700',
      icon: 'i-heroicons-exclamation-triangle',
      iconColor: 'text-yellow-400',
      title: 'text-yellow-800 font-medium',
      message: 'text-yellow-700',
      closeButton: 'text-yellow-400 hover:text-yellow-600',
    },
    info: {
      container: 'bg-blue-50 border border-blue-200 text-blue-700',
      icon: 'i-heroicons-information-circle',
      iconColor: 'text-blue-400',
      title: 'text-blue-800 font-medium',
      message: 'text-blue-700',
      closeButton: 'text-blue-400 hover:text-blue-600',
    },
  };

  return configs[props.type];
});

const alertClasses = computed(() => [
  'px-4 py-3 rounded',
  alertConfig.value.container,
]);

const iconName = computed(() => alertConfig.value.icon);
const iconClasses = computed(() => ['w-5 h-5', alertConfig.value.iconColor]);
const titleClasses = computed(() => ['text-sm', alertConfig.value.title]);
const messageClasses = computed(() => ['text-sm', alertConfig.value.message, props.title ? 'mt-1' : '']);
const closeButtonClasses = computed(() => [
  'inline-flex rounded-md p-1.5 focus:outline-none focus:ring-2 focus:ring-offset-2',
  alertConfig.value.closeButton,
]);
</script>
