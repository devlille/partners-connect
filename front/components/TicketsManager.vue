<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h3 class="text-lg font-semibold text-gray-900">Tickets</h3>
        <p class="text-sm text-gray-600 mt-1">
          {{ tickets.length > 0 ? `${tickets.length} ticket${tickets.length > 1 ? 's' : ''}` : 'Gérez les tickets pour ce partenariat' }}
        </p>
      </div>
      <UButton color="primary" icon="i-heroicons-plus" :disabled="loading" @click="openAddModal">
        Ajouter des tickets
      </UButton>
    </div>

    <TableSkeleton v-if="loading" :columns="5" :rows="3" />
    <AlertMessage v-else-if="error" :message="error" type="error" />

    <UTable v-else-if="tickets.length > 0" :data="tickets" :columns="columns" />

    <div
      v-else
      class="text-center py-12 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300"
    >
      <i class="i-heroicons-ticket text-4xl text-gray-400 mb-3" />
      <h3 class="text-sm font-medium text-gray-900 mb-1">Aucun ticket</h3>
      <p class="text-sm text-gray-500 mb-4">
        Commencez par ajouter des tickets pour ce partenariat
      </p>
      <UButton color="primary" icon="i-heroicons-plus" @click="openAddModal">
        Ajouter des tickets
      </UButton>
    </div>

    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="isModalOpen"
          class="fixed inset-0 z-50 flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="modalTitleId"
        >
          <!-- Overlay -->
          <div
            class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
            aria-hidden="true"
            @click="closeModal"
          />

          <!-- Modal -->
          <div
            class="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto transform transition-all"
          >
            <!-- Header -->
            <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <h3 :id="modalTitleId" class="text-lg font-semibold text-gray-900">
                {{ editingTicket ? 'Modifier le ticket' : 'Ajouter des tickets' }}
              </h3>
              <button
                type="button"
                aria-label="Fermer"
                :disabled="saving"
                class="text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
                @click="closeModal"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>

            <!-- Content -->
            <form id="ticket-form" class="px-6 py-4 space-y-4" @submit.prevent="handleSubmit">
              <!-- Mode édition -->
              <template v-if="editingTicket">
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label
                      for="edit_first_name"
                      class="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Prénom <span class="text-red-500">*</span>
                    </label>
                    <UInput
                      id="edit_first_name"
                      v-model="singleForm.first_name"
                      placeholder="John"
                      :disabled="saving"
                    />
                  </div>
                  <div>
                    <label
                      for="edit_last_name"
                      class="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Nom <span class="text-red-500">*</span>
                    </label>
                    <UInput
                      id="edit_last_name"
                      v-model="singleForm.last_name"
                      placeholder="Doe"
                      :disabled="saving"
                    />
                  </div>
                </div>
              </template>

              <!-- Mode ajout (multi-lignes) -->
              <template v-else>
                <div class="space-y-3">
                  <div class="grid grid-cols-[1fr_1fr_2.5rem] gap-3">
                    <span class="text-sm font-medium text-gray-700">
                      Prénom <span class="text-red-500">*</span>
                    </span>
                    <span class="text-sm font-medium text-gray-700">
                      Nom <span class="text-red-500">*</span>
                    </span>
                    <span />
                  </div>
                  <div
                    v-for="(row, index) in batchRows"
                    :key="index"
                    class="grid grid-cols-[1fr_1fr_2.5rem] gap-3 items-center"
                  >
                    <UInput v-model="row.first_name" placeholder="John" :disabled="saving" />
                    <UInput v-model="row.last_name" placeholder="Doe" :disabled="saving" />
                    <button
                      v-if="batchRows.length > 1"
                      type="button"
                      :disabled="saving"
                      class="flex items-center justify-center w-8 h-8 rounded text-gray-400 hover:text-red-500 transition-colors disabled:opacity-50"
                      aria-label="Supprimer cette ligne"
                      @click="removeBatchRow(index)"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                        />
                      </svg>
                    </button>
                    <span v-else />
                  </div>
                </div>
                <button
                  type="button"
                  :disabled="saving"
                  class="flex items-center gap-2 text-sm font-medium text-primary-600 hover:text-primary-800 transition-colors disabled:opacity-50"
                  @click="addBatchRow"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M12 4v16m8-8H4"
                    />
                  </svg>
                  Ajouter une ligne
                </button>
              </template>

              <AlertMessage v-if="formError" :message="formError" type="error" />
            </form>

            <!-- Footer -->
            <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <UButton
                type="button"
                color="neutral"
                variant="ghost"
                :disabled="saving"
                @click="closeModal"
              >
                Annuler
              </UButton>
              <UButton
                type="submit"
                form="ticket-form"
                color="primary"
                :loading="saving"
                :disabled="saving"
              >
                {{ submitLabel }}
              </UButton>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { h, resolveComponent } from 'vue';
import type { TicketSchema, TicketDataSchema } from '~/utils/api';
import {
  getEventsPartnershipTickets,
  postEventsPartnershipTickets,
  putEventsPartnershipTickets,
} from '~/utils/api';

interface Props {
  eventSlug: string;
  partnershipId: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{ ticketsUpdated: [tickets: TicketSchema[]] }>();

const toast = useToast();

const tickets = ref<TicketSchema[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

const isModalOpen = ref(false);
const editingTicket = ref<TicketSchema | null>(null);
const saving = ref(false);
const formError = ref<string | null>(null);

const singleForm = ref<TicketDataSchema>({ first_name: '', last_name: '' });
const batchRows = ref<TicketDataSchema[]>([{ first_name: '', last_name: '' }]);

const modalTitleId = `tickets-modal-${Math.random().toString(36).substr(2, 9)}`;

const submitLabel = computed(() => {
  if (editingTicket.value) return 'Modifier';
  const count = batchRows.value.length;
  return count > 1 ? `Ajouter ${count} tickets` : 'Ajouter le ticket';
});

const columns = [
  {
    header: 'Prénom',
    id: 'first_name',
    cell: (info: any) => info.row.original.data.first_name,
  },
  {
    header: 'Nom',
    id: 'last_name',
    cell: (info: any) => info.row.original.data.last_name,
  },
  {
    header: 'ID Externe',
    accessorKey: 'external_id',
    cell: (info: any) =>
      h('span', { class: 'text-sm text-gray-500 font-mono' }, info.row.original.external_id),
  },
  {
    header: 'Lien',
    accessorKey: 'url',
    cell: (info: any) => {
      const url = info.row.original.url;
      if (!url) return h('span', { class: 'text-gray-400' }, '-');
      return h(
        'a',
        {
          href: url,
          target: '_blank',
          rel: 'noopener noreferrer',
          class: 'text-primary-600 hover:text-primary-900 flex items-center gap-1 text-sm',
        },
        ['Voir le ticket', h('i', { class: 'i-heroicons-arrow-top-right-on-square text-xs' })],
      );
    },
  },
];

async function loadTickets() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getEventsPartnershipTickets(props.eventSlug, props.partnershipId);
    tickets.value = response.data;
  } catch {
    error.value = 'Impossible de charger les tickets';
  } finally {
    loading.value = false;
  }
}

function openAddModal() {
  editingTicket.value = null;
  batchRows.value = [{ first_name: '', last_name: '' }];
  formError.value = null;
  isModalOpen.value = true;
}

function openEditModal(ticket: TicketSchema) {
  editingTicket.value = ticket;
  singleForm.value = { first_name: ticket.data.first_name, last_name: ticket.data.last_name };
  formError.value = null;
  isModalOpen.value = true;
}

function closeModal() {
  if (saving.value) return;
  isModalOpen.value = false;
  editingTicket.value = null;
  formError.value = null;
}

function addBatchRow() {
  batchRows.value.push({ first_name: '', last_name: '' });
}

function removeBatchRow(index: number) {
  batchRows.value.splice(index, 1);
}

async function handleSubmit() {
  formError.value = null;

  if (editingTicket.value) {
    if (!singleForm.value.first_name.trim() || !singleForm.value.last_name.trim()) {
      formError.value = 'Le prénom et le nom sont obligatoires';
      return;
    }
    await updateTicket();
  } else {
    if (batchRows.value.some((r) => !r.first_name.trim() || !r.last_name.trim())) {
      formError.value = 'Le prénom et le nom sont obligatoires pour chaque ticket';
      return;
    }
    await createTickets();
  }
}

async function updateTicket() {
  let success = false;
  try {
    saving.value = true;
    await putEventsPartnershipTickets(
      props.eventSlug,
      props.partnershipId,
      editingTicket.value!.id,
      singleForm.value,
    );
    toast.add({ title: 'Succès', description: 'Le ticket a été modifié avec succès', color: 'success' });
    await loadTickets();
    emit('ticketsUpdated', tickets.value);
    success = true;
  } catch {
    formError.value = 'Impossible de modifier le ticket';
    toast.add({ title: 'Erreur', description: formError.value, color: 'error' });
  } finally {
    saving.value = false;
    if (success) closeModal();
  }
}

async function createTickets() {
  let success = false;
  try {
    saving.value = true;
    const response = await postEventsPartnershipTickets(
      props.eventSlug,
      props.partnershipId,
      batchRows.value,
    );
    const count = response.data.tickets.length;
    toast.add({
      title: 'Succès',
      description: `${count} ticket${count > 1 ? 's ont été ajoutés' : ' a été ajouté'} avec succès`,
      color: 'success',
    });
    await loadTickets();
    emit('ticketsUpdated', tickets.value);
    success = true;
  } catch (err: any) {
    formError.value =
      err?.response?.data?.message ??
      (batchRows.value.length > 1 ? "Impossible d'ajouter les tickets" : "Impossible d'ajouter le ticket");
    toast.add({ title: 'Erreur', description: formError.value, color: 'error' });
  } finally {
    saving.value = false;
    if (success) closeModal();
  }
}

onMounted(loadTickets);

defineExpose({ loadTickets });
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .relative,
.modal-leave-active .relative {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
  opacity: 0;
}
</style>
