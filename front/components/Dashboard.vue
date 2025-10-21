<template>
  <div class="flex min-h-screen bg-gray-50">
    <!-- Sidebar -->
    <aside class="w-64 bg-white border-r border-gray-200">
      <div class="p-4 border-b border-gray-200">
        <h2 class="text-xl font-bold text-gray-900">{{ title }}</h2>
      </div>

      <nav class="p-4 space-y-2">
        <UButton
          v-for="link in mainLinks"
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

      <div class="border-t border-gray-200 my-4" />

      <nav class="p-4 space-y-2">
        <UButton
          v-for="link in footerLinks"
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

    <!-- Main Panel -->
    <main class="flex-1 overflow-auto">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
interface Link {
  label: string
  icon: string
  to: string
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
