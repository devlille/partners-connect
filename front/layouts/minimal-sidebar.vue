<template>
  <div class="flex min-h-screen bg-gray-50">
    <!-- Sidebar for public pages with navigation -->
    <aside class="w-64 bg-white border-r border-gray-200">
      <div class="p-4 border-b border-gray-200">
        <h2 class="text-xl font-bold text-gray-900">{{ sidebarTitle }}</h2>
      </div>

      <nav class="p-4 space-y-2" role="navigation" aria-label="Menu principal">
        <div v-for="link in sidebarLinks" :key="link.to" class="relative">
          <UButton
            :to="link.to"
            :icon="link.icon"
            :label="link.label"
            color="neutral"
            variant="ghost"
            block
            class="justify-start"
          />
          <span
            v-if="link.badge"
            :title="link.badge.title"
            class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center justify-center w-5 h-5 text-xs font-bold text-white rounded-full"
            :class="{
              'bg-red-600': link.badge.color === 'error',
              'bg-yellow-600': link.badge.color === 'warning',
              'bg-green-600': link.badge.color === 'success',
              'bg-blue-600': link.badge.color === 'info'
            }"
          >
            {{ link.badge.label }}
          </span>
        </div>
      </nav>
    </aside>

    <!-- Main content -->
    <div class="flex-1 flex flex-col">
      <main class="flex-1">
        <slot />
      </main>

      <Footer />
    </div>
  </div>
</template>

<script lang="ts" setup>
interface SidebarLink {
  label: string
  icon: string
  to: string
  badge?: {
    label: string
    color: 'error' | 'warning' | 'success' | 'info'
    title?: string
  }
}

interface Props {
  sidebarTitle?: string
  sidebarLinks?: SidebarLink[]
}

withDefaults(defineProps<Props>(), {
  sidebarTitle: 'DevLille',
  sidebarLinks: () => []
})
</script>

<style></style>
