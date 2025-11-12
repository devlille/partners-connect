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
          <PageTitle>Événements Favoris</PageTitle>
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
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import authMiddleware from "~/middleware/auth";
import type { TableRow } from "@nuxt/ui";
import type { FavoriteEvent } from "~/composables/useFavoriteEvents";

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
    cell: (info: any) => {
      const fav = info.row.original;
      const container = h('div', { class: 'flex gap-2 items-center' }, [
        h(resolveComponent('UButton'), {
          onClick: () => {
            navigateTo(`/orgs/${fav.orgSlug}/events/${fav.eventSlug}`);
          },
          icon: 'i-heroicons-arrow-right-circle',
          size: 'md',
          color: 'primary',
          variant: 'ghost',
          square: true,
          title: 'Voir l\'événement'
        }),
        h(resolveComponent('UButton'), {
          onClick: (e: Event) => {
            e.stopPropagation();
            removeFavorite(fav.orgSlug, fav.eventSlug);
          },
          icon: 'i-heroicons-trash',
          size: 'md',
          color: 'red',
          variant: 'ghost',
          square: true,
          title: 'Retirer des favoris'
        })
      ]);
      return container;
    }
  }
];

useHead({
  title: "Événements Favoris | DevLille"
});
</script>
