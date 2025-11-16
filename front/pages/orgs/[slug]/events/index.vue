<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <UButton
            :to="`/orgs/${slug}`"
            icon="i-heroicons-arrow-left"
            color="neutral"
            variant="ghost"
            class="mb-2"
            label="Retour"
          />
          <PageTitle>Événements - {{ organisationName }}</PageTitle>
        </div>
        <UButton
          :to="`/orgs/${slug}/events/create`"
          label="Créer un événement"
          icon="i-heroicons-plus"
          color="primary"
        />
      </div>
    </div>

    <div class="p-6">
      <TableSkeleton v-if="loading" :columns="5" :rows="8" />

      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <UTable
        v-else
        :data="data"
        :columns="columns"
      />
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsEvents, getOrgs, type EventSummary } from "~/utils/api";
import authMiddleware from "~/middleware/auth";
import type {TableRow} from "@nuxt/ui";

const { footerLinks } = useDashboardLinks();
const { getOrgSlug } = useRouteParams();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});
const {formatDate} = useDateFormatter();
const { isFavorite, toggleFavorite } = useFavoriteEvents();

const slug = computed(() => getOrgSlug());
const data = ref<EventSummary[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const organisationName = ref<string>('');

const columns = computed(() => [
  {
    header: 'Nom',
    accessorKey: 'name',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${slug.value}/events/${event.slug}`),
        class: 'cursor-pointer hover:underline'
      }, info.getValue('name'));
    }
  },
  {
    header: 'Date de début',
    accessorKey: 'start_time',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${slug.value}/events/${event.slug}`),
        class: 'cursor-pointer'
      }, formatDate(info.getValue('start_time')));
    }
  },
  {
    header: 'Date de fin',
    accessorKey: 'end_time',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${slug.value}/events/${event.slug}`),
        class: 'cursor-pointer'
      }, formatDate(info.getValue('end_time')));
    }
  },
  {
    header: 'Date de début CFP',
    accessorKey: 'submission_start_time',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${slug.value}/events/${event.slug}`),
        class: 'cursor-pointer'
      }, formatDate(info.getValue('submission_start_time')));
    }
  },
  {
    header: 'Date de fin CFP',
    accessorKey: 'submission_end_time',
    cell: (info: TableRow<EventSummary>) => {
      const event = info.row.original;
      return h('div', {
        onClick: () => navigateTo(`/orgs/${slug.value}/events/${event.slug}`),
        class: 'cursor-pointer'
      }, formatDate(info.getValue('submission_end_time')));
    }
  },
  {
    header: 'Favori',
    accessorKey: 'slug',
    cell: (info: any) => {
      const event = info.row.original;
      return h('button', {
        onClick: (e: Event) => {
          e.stopPropagation();
          toggleFavorite({
            orgSlug: slug.value,
            orgName: organisationName.value,
            eventSlug: event.slug,
            eventName: event.name
          });
        },
        class: 'text-2xl hover:scale-110 transition-transform',
        title: isFavorite(slug.value, event.slug) ? 'Retirer des favoris' : 'Ajouter aux favoris'
      }, isFavorite(slug.value, event.slug) ? '⭐' : '☆');
    }
  }
]);

// Menu contextuel pour la page d'événements de l'organisation
const orgLinks = computed(() => [
  {
    label: 'Informations',
    icon: 'i-heroicons-information-circle',
    to: `/orgs/${slug.value}`
  },
  {
    label: 'Événements',
    icon: 'i-heroicons-calendar',
    to: `/orgs/${slug.value}/events`
  },
  {
    label: 'Utilisateurs',
    icon: 'i-heroicons-users',
    to: `/orgs/${slug.value}/users`
  },
  {
    label: 'Partenariats',
    icon: 'i-heroicons-handshake',
    to: `/orgs/${slug.value}/partnerships`
  }
]);

async function loadEvents() {
  try {
    loading.value = true;
    error.value = null;

    // Charger le nom de l'organisation
    const orgResponse = await getOrgs(slug.value);
    organisationName.value = orgResponse.data.name;

    // Charger les événements
    const response = await getOrgsEvents(slug.value);
    data.value = response.data.items;
  } catch (err) {
    console.error('Failed to load events:', err);
    error.value = 'Impossible de charger les événements';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadEvents();
});

// Recharger si le slug change
watch(slug, () => {
  loadEvents();
});

useHead({
  title: computed(() => `Événements - ${organisationName.value || 'Organisation'} | DevLille`)
});
</script>
