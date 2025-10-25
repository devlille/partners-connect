<template>
  <div class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
    <!-- Header avec nom de l'entreprise -->
    <div class="flex items-start justify-between mb-3">
      <div class="flex-1">
        <h3 class="font-semibold text-gray-900">{{ item.company_name }}</h3>
        <p v-if="item.publication_date" class="text-sm text-gray-600 mt-1">
          <i class="i-heroicons-calendar-days text-xs mr-1" />
          {{ formatDate(item.publication_date) }}
        </p>
      </div>

      <!-- Badge de statut -->
      <span :class="statusBadgeClass" class="px-2 py-1 text-xs font-medium rounded-full">
        {{ statusLabel }}
      </span>
    </div>

    <!-- Support visuel preview -->
    <div v-if="item.support_url" class="mb-3">
      <img
        :src="item.support_url"
        :alt="`Support visuel ${item.company_name}`"
        class="w-full h-32 object-cover rounded border border-gray-200"
      />
    </div>

    <!-- Actions -->
    <div class="flex gap-2">
      <UButton
        v-if="status !== 'done'"
        size="sm"
        variant="ghost"
        color="neutral"
        icon="i-heroicons-calendar"
        @click="$emit('schedule', item)"
      >
        {{ item.publication_date ? 'Modifier la date' : 'Planifier' }}
      </UButton>

      <UButton
        size="sm"
        variant="ghost"
        color="neutral"
        icon="i-heroicons-photo"
        @click="$emit('upload', item)"
      >
        {{ item.support_url ? 'Changer' : 'Ajouter' }}
      </UButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CommunicationItemSchema } from '~/utils/api';

interface Props {
  item: CommunicationItemSchema;
  status: 'done' | 'planned' | 'unplanned';
}

const props = defineProps<Props>();

defineEmits<{
  schedule: [item: CommunicationItemSchema];
  upload: [item: CommunicationItemSchema];
}>();

const statusLabel = computed(() => {
  switch (props.status) {
    case 'done':
      return 'Terminée';
    case 'planned':
      return 'Planifiée';
    case 'unplanned':
      return 'Non planifiée';
    default:
      return '';
  }
});

const statusBadgeClass = computed(() => {
  switch (props.status) {
    case 'done':
      return 'bg-green-100 text-green-800';
    case 'planned':
      return 'bg-blue-100 text-blue-800';
    case 'unplanned':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
});

function formatDate(dateString: string) {
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  }).format(date);
}
</script>
