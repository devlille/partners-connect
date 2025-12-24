<template>
  <Transition
    enter-active-class="transition-all duration-300 ease-out"
    enter-from-class="opacity-0 -translate-y-2"
    enter-to-class="opacity-100 translate-y-0"
    leave-active-class="transition-all duration-200 ease-in"
    leave-from-class="opacity-100 translate-y-0"
    leave-to-class="opacity-0 -translate-y-2"
  >
    <div
      v-if="!dismissed"
      :class="[
        borderless ? '' : 'mt-4 pt-4 border-t border-gray-200',
      ]"
    >
      <div
        :class="[
          'rounded-lg border',
          sizeClasses.padding,
          variantClasses.bg,
          variantClasses.border,
        ]"
        :role="variant === 'error' ? 'alert' : 'status'"
        :aria-live="variant === 'error' ? 'assertive' : 'polite'"
      >
        <div class="flex items-start gap-3">
          <!-- Icon -->
          <i
            v-if="icon"
            :class="[
              icon,
              sizeClasses.icon,
              variantClasses.icon,
              'shrink-0 mt-0.5',
            ]"
            aria-hidden="true"
          />

          <!-- Content -->
          <div class="flex-1 min-w-0">
            <!-- Header with title and dismiss button -->
            <div class="flex items-start justify-between gap-2">
              <button
                v-if="collapsible"
                type="button"
                class="flex items-center gap-1 focus:outline-none focus:ring-2 focus:ring-offset-1 rounded"
                :class="variantClasses.title"
                :aria-expanded="!collapsed"
                @click="collapsed = !collapsed"
              >
                <h3 :class="[sizeClasses.title, 'font-semibold']">
                  {{ title }}
                </h3>
                <i
                  :class="[
                    collapsed ? 'i-heroicons-chevron-down' : 'i-heroicons-chevron-up',
                    'text-sm transition-transform',
                  ]"
                  aria-hidden="true"
                />
              </button>
              <h3 v-else :class="[sizeClasses.title, 'font-semibold mb-1', variantClasses.title]">
                {{ title }}
              </h3>

              <button
                v-if="dismissible"
                type="button"
                :class="[
                  'shrink-0 p-1 rounded hover:bg-black/5 focus:outline-none focus:ring-2 focus:ring-offset-1 transition-colors',
                  variantClasses.dismissButton,
                ]"
                :aria-label="dismissLabel"
                @click="dismiss"
              >
                <i class="i-heroicons-x-mark" :class="sizeClasses.dismissIcon" aria-hidden="true" />
              </button>
            </div>

            <!-- Collapsible content -->
            <Transition
              enter-active-class="transition-all duration-200 ease-out"
              enter-from-class="opacity-0 max-h-0"
              enter-to-class="opacity-100 max-h-96"
              leave-active-class="transition-all duration-150 ease-in"
              leave-from-class="opacity-100 max-h-96"
              leave-to-class="opacity-0 max-h-0"
            >
              <div v-show="!collapsed" class="overflow-hidden">
                <div
                  :class="[sizeClasses.content, variantClasses.content, collapsible ? 'mt-2' : '']"
                >
                  <slot />
                </div>

                <!-- Actions slot -->
                <div v-if="$slots.actions" class="mt-3 flex items-center gap-2">
                  <slot name="actions" />
                </div>
              </div>
            </Transition>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
export type NoticeVariant = "info" | "success" | "warning" | "error";
export type NoticeSize = "sm" | "md" | "lg";

const props = withDefaults(
  defineProps<{
    title: string;
    variant?: NoticeVariant;
    icon?: string;
    dismissible?: boolean;
    dismissLabel?: string;
    collapsible?: boolean;
    defaultCollapsed?: boolean;
    borderless?: boolean;
    size?: NoticeSize;
  }>(),
  {
    variant: "info",
    icon: undefined,
    dismissible: false,
    dismissLabel: "Fermer",
    collapsible: false,
    defaultCollapsed: false,
    borderless: false,
    size: "md",
  }
);

const emit = defineEmits<{
  dismiss: [];
}>();

const dismissed = ref(false);
const collapsed = ref(props.defaultCollapsed);

const variantClasses = computed(() => {
  const variants = {
    info: {
      bg: "bg-blue-50",
      border: "border-blue-200",
      title: "text-blue-900",
      content: "text-blue-700",
      icon: "text-blue-600",
      dismissButton: "text-blue-600 hover:text-blue-800 focus:ring-blue-500",
    },
    success: {
      bg: "bg-green-50",
      border: "border-green-200",
      title: "text-green-900",
      content: "text-green-700",
      icon: "text-green-600",
      dismissButton: "text-green-600 hover:text-green-800 focus:ring-green-500",
    },
    warning: {
      bg: "bg-amber-50",
      border: "border-amber-200",
      title: "text-amber-900",
      content: "text-amber-700",
      icon: "text-amber-600",
      dismissButton: "text-amber-600 hover:text-amber-800 focus:ring-amber-500",
    },
    error: {
      bg: "bg-red-50",
      border: "border-red-200",
      title: "text-red-900",
      content: "text-red-700",
      icon: "text-red-600",
      dismissButton: "text-red-600 hover:text-red-800 focus:ring-red-500",
    },
  };
  return variants[props.variant];
});

const sizeClasses = computed(() => {
  const sizes = {
    sm: {
      padding: "p-3",
      title: "text-xs",
      content: "text-xs",
      icon: "text-lg",
      dismissIcon: "text-sm",
    },
    md: {
      padding: "p-4",
      title: "text-sm",
      content: "text-sm",
      icon: "text-xl",
      dismissIcon: "text-base",
    },
    lg: {
      padding: "p-5",
      title: "text-base",
      content: "text-base",
      icon: "text-2xl",
      dismissIcon: "text-lg",
    },
  };
  return sizes[props.size];
});

function dismiss() {
  dismissed.value = true;
  emit("dismiss");
}

// Expose methods for external control
defineExpose({
  dismiss,
  collapsed,
  dismissed,
});
</script>
