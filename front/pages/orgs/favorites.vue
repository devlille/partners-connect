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
      <div
        v-if="favorites.length === 0"
        class="bg-gray-50 border border-gray-200 text-gray-700 px-4 py-8 rounded text-center"
      >
        <i class="i-heroicons-star text-4xl text-gray-400 mb-2" />
        <p class="text-lg font-medium mb-1">Aucun événement favori</p>
        <p class="text-sm text-gray-500">
          Ajoutez des événements à vos favoris pour y accéder rapidement depuis cette page.
        </p>
      </div>

      <UTable v-else :data="favorites" :columns="columns" />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import authMiddleware from "~/middleware/auth";
import type { TableRow } from "@nuxt/ui";
import type { EventSummary } from "~/utils/api";

const { mainLinks, footerLinks } = useDashboardLinks();
const { favorites, removeFavorite } = useFavoriteEvents();
const { formatDate } = useDateFormatter();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const columns = [
  {
    header: 'Événement',
    accessorKey: 'name',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${event.org_slug}/events/${event.slug}`),
        class: 'cursor-pointer hover:underline',
      }, info.getValue('name'));
    },
  },
  {
    header: 'Organisation',
    accessorKey: 'org_name',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${event.org_slug}/events/${event.slug}`),
        class: 'cursor-pointer',
      }, info.getValue('org_name'));
    },
  },
  {
    header: "Date de l'événement",
    accessorKey: 'start_time',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${event.org_slug}/events/${event.slug}`),
        class: 'cursor-pointer',
      }, formatDate(info.getValue('start_time') as string));
    },
  },
  {
    header: 'Actions',
    accessorKey: 'slug',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h(resolveComponent('UButton'), {
        onClick: (e: Event) => {
          e.stopPropagation();
          removeFavorite(event.slug);
        },
        icon: 'i-heroicons-trash',
        size: 'md',
        color: 'red',
        variant: 'ghost',
        square: true,
        title: 'Retirer des favoris',
      });
    },
  },
];

useHead({
  title: "Événements Favoris | DevLille",
});
</script>
