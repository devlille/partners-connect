<template>
  <h1 :class="titleClasses">
    <slot />
  </h1>
</template>

<script setup lang="ts">
interface Props {
  /** Taille du titre (default: xl) */
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  /** Couleur du titre (default: gray-900) */
  color?: 'gray-900' | 'gray-800' | 'gray-700' | 'white';
  /** Classes CSS additionnelles */
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  size: '2xl',
  color: 'gray-900'
});

const sizeClasses = {
  'sm': 'text-sm',
  'md': 'text-base',
  'lg': 'text-lg',
  'xl': 'text-xl',
  '2xl': 'text-2xl'
};

const colorClasses = {
  'gray-900': 'text-gray-900',
  'gray-800': 'text-gray-800',
  'gray-700': 'text-gray-700',
  'white': 'text-white'
};

const titleClasses = computed(() => [
  'font-bold',
  sizeClasses[props.size],
  colorClasses[props.color],
  props.class
]);
</script>
