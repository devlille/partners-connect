<template>
  <div class="flex min-h-screen bg-gray-50">
    <!-- Sidebar -->
    <aside class="w-64 bg-white border-r border-gray-200">
      <div class="p-4 border-b border-gray-200">
        <h2 class="text-xl font-bold text-gray-900">{{ title }}</h2>
      </div>

      <nav class="p-4 space-y-2">
        <NuxtLink
          v-for="link in mainLinks"
          :key="link.to"
          :to="link.to"
          class="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 transition-colors"
          active-class="bg-gray-100 text-gray-900"
        >
          <i :class="link.icon" class="text-lg" aria-hidden="true" />
          <span class="flex-1">{{ link.label }}</span>
          <span
            v-if="link.badge"
            :title="link.badge.title"
            class="inline-flex items-center justify-center w-5 h-5 text-xs font-bold rounded-full"
            :class="{
              'bg-red-100 text-red-700': link.badge.color === 'error',
              'bg-yellow-100 text-yellow-700': link.badge.color === 'warning',
              'bg-blue-100 text-blue-700': link.badge.color === 'info',
              'bg-green-100 text-green-700': link.badge.color === 'success',
            }"
          >
            {{ link.badge.label }}
          </span>
        </NuxtLink>
      </nav>

      <div class="border-t border-gray-200 my-4" />

      <nav class="p-4 space-y-2">
        <component
          :is="link.click ? 'button' : NuxtLink"
          v-for="link in footerLinks"
          :key="link.to"
          :to="link.click ? undefined : link.to"
          class="flex items-center gap-2 px-3 py-2 text-sm font-medium text-gray-700 rounded-md hover:bg-gray-100 transition-colors w-full text-left"
          @click="link.click"
        >
          <i :class="link.icon" class="text-lg" aria-hidden="true" />
          <span>{{ link.label }}</span>
        </component>
      </nav>
    </aside>

    <!-- Main Panel -->
    <main class="flex-1 overflow-auto">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { NuxtLink } from '#components'

interface LinkBadge {
  label: string
  color: 'error' | 'warning' | 'info' | 'success'
  title?: string
}

interface Link {
  label: string
  icon: string
  to: string
  click?: () => void
  badge?: LinkBadge
}

interface Props {
  title?: string
  mainLinks: Link[]
  footerLinks?: Link[]
}

withDefaults(defineProps<Props>(), {
  title: 'DevLille',
  footerLinks: () => []
})
</script>
