<template>
  <Dashboard :main-links="mainLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            to="/orgs"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <h1 class="text-2xl font-bold text-gray-900">Événements Favoris</h1>
        </div>
      </div>
    </div>

    <div class="p-6">
      <div v-if="favoritesList.length === 0" class="bg-gray-50 border border-gray-200 text-gray-700 px-4 py-8 rounded text-center">
        <i class="i-heroicons-star text-4xl text-gray-400 mb-2" />
        <p class="text-lg font-medium mb-1">Aucun événement favori</p>
        <p class="text-sm text-gray-500">Ajoutez des événements à vos favoris pour y accéder rapidement depuis cette page.</p>
      </div>

      <UTable
        v-else
        :data="favoritesList"
        :columns="columns"
        @select="onSelectEvent"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import authMiddleware from "~/middleware/auth";
import type { TableRow } from "@nuxt/ui";
import type { FavoriteEvent } from "~/composables/useFavoriteEvents";

const router = useRouter();
const { mainLinks, footerLinks } = useDashboardLinks();
const { getFavorites, removeFavorite } = useFavoriteEvents();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const favoritesList = computed(() => getFavorites());

const columns = [
  {
    header: 'Événement',
    accessorKey: 'eventName',
    cell: (info: TableRow<FavoriteEvent>) => info.getValue('eventName')
  },
  {
    header: 'Organisation',
    accessorKey: 'orgName',
    cell: (info: TableRow<FavoriteEvent>) => info.getValue('orgName')
  },
  {
    header: 'Ajouté le',
    accessorKey: 'addedAt',
    cell: (info: TableRow<FavoriteEvent>) => {
      const date = new Date(info.getValue('addedAt') as string);
      return date.toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
  },
  {
    header: 'Actions',
    accessorKey: 'eventSlug',
    cell: (info: TableRow<FavoriteEvent>) => {
      const fav = info.row.original;
      return h('button', {
        onClick: (e: Event) => {
          e.stopPropagation();
          removeFavorite(fav.orgSlug, fav.eventSlug);
        },
        class: 'text-red-600 hover:text-red-800 hover:scale-110 transition-transform text-xl',
        title: 'Retirer des favoris'
      }, '🗑️');
    }
  }
];

const onSelectEvent = (row: TableRow<FavoriteEvent>) => {
  const fav = row.original;
  router.push(`/orgs/${fav.orgSlug}/events/${fav.eventSlug}`);
};

useHead({
  title: "Événements Favoris | DevLille"
});
</script>
