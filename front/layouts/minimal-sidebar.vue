<template>
  <div class="flex min-h-screen bg-gray-50">
    <!-- Sidebar for public pages with navigation -->
    <aside class="w-64 bg-white border-r border-gray-200">
      <div class="p-4 border-b border-gray-200">
        <h2 class="text-xl font-bold text-gray-900">{{ sidebarTitle }}</h2>
      </div>

      <nav class="p-4 space-y-2" role="navigation" aria-label="Menu principal">
        <UButton
          v-for="link in sidebarLinks"
          :key="link.to"
          :to="link.to"
          :icon="link.icon"
          :label="link.label"
          color="neutral"
          variant="ghost"
          block
          class="justify-start"
        />
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
