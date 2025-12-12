<template>
  <div
    class="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden"
    role="status"
    :aria-label="ARIA_LABELS.LOADING"
  >
    <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
      <!-- Header -->
      <thead class="bg-gray-50 dark:bg-gray-900">
        <tr>
          <th v-for="col in columns" :key="col" class="px-6 py-3 text-left">
            <div
              class="h-4 bg-gray-300 dark:bg-gray-600 rounded animate-pulse"
              :style="{ width: `${60 + Math.random() * 40}%` }"
            />
          </th>
        </tr>
      </thead>

      <!-- Body -->
      <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
        <tr v-for="row in rows" :key="row">
          <td v-for="col in columns" :key="`${row}-${col}`" class="px-6 py-4 whitespace-nowrap">
            <div
              class="h-4 bg-gray-200 dark:bg-gray-700 rounded animate-pulse"
              :style="{
                width: `${40 + Math.random() * 50}%`,
                animationDelay: `${(row * columns + col) * 50}ms`
              }"
            />
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Screen reader text -->
    <span class="sr-only">{{ ARIA_LABELS.LOADING }}</span>
  </div>
</template>

<script setup lang="ts">
import { ARIA_LABELS } from '~/constants/accessibility';

withDefaults(defineProps<{
  /** Nombre de colonnes à afficher */
  columns?: number;
  /** Nombre de lignes à afficher */
  rows?: number;
}>(), {
  columns: 4,
  rows: 5,
});
</script>

<style scoped>
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.animate-pulse {
  animation: pulse 1.5s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
