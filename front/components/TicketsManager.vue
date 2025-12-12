<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h3 class="text-lg font-semibold text-gray-900">Tickets</h3>
        <p class="text-sm text-gray-600 mt-1">Gérez les tickets pour ce partenariat</p>
      </div>
      <UButton
        color="primary"
        icon="i-heroicons-plus"
        @click="openAddTicketModal"
        :disabled="loading"
      >
        Ajouter un ticket
      </UButton>
    </div>

    <!-- Loading state -->
    <TableSkeleton v-if="loading" :columns="4" :rows="3" />

    <!-- Error state -->
    <AlertMessage v-else-if="error" :message="error" type="error" />

    <!-- Tickets list -->
    <div v-else-if="tickets.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <th
              scope="col"
              class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              Prénom
            </th>
            <th
              scope="col"
              class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              Nom
            </th>
            <th
              scope="col"
              class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              ID Externe
            </th>
            <th
              scope="col"
              class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              URL
            </th>
            <th
              scope="col"
              class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
            >
              Actions
            </th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="ticket in tickets" :key="ticket.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ ticket.data.first_name }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ ticket.data.last_name }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
              {{ ticket.external_id }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm">
              <a
                v-if="ticket.url"
                :href="ticket.url"
                target="_blank"
                rel="noopener noreferrer"
                class="text-primary-600 hover:text-primary-900 flex items-center gap-1"
              >
                Voir
                <i class="i-heroicons-arrow-top-right-on-square text-xs" />
              </a>
              <span v-else class="text-gray-400">-</span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
              <UButton
                size="sm"
                color="primary"
                variant="ghost"
                icon="i-heroicons-pencil"
                @click="openEditTicketModal(ticket)"
              >
                Modifier
              </UButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty state -->
    <div
      v-else
      class="text-center py-12 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300"
    >
      <i class="i-heroicons-ticket text-4xl text-gray-400 mb-3" />
      <h3 class="text-sm font-medium text-gray-900 mb-1">Aucun ticket</h3>
      <p class="text-sm text-gray-500 mb-4">
        Commencez par ajouter un premier ticket pour ce partenariat
      </p>
      <UButton color="primary" icon="i-heroicons-plus" @click="openAddTicketModal">
        Ajouter un ticket
      </UButton>
    </div>

    <!-- Modal d'ajout/modification de ticket -->
    <Teleport to="body">
      <div
        v-if="isModalOpen"
        class="fixed inset-0 z-50 overflow-y-auto"
        aria-labelledby="modal-title"
        role="dialog"
        aria-modal="true"
      >
        <div
          class="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0"
        >
          <!-- Background overlay -->
          <div
            class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
            aria-hidden="true"
            @click="closeModal"
          ></div>

          <!-- Center modal -->
          <span class="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true"
            >&#8203;</span
          >

          <div
            class="inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6"
          >
            <div>
              <div class="mt-3 sm:mt-0">
                <h3 class="text-lg leading-6 font-medium text-gray-900 mb-4" id="modal-title">
                  {{ editingTicket ? 'Modifier le ticket' : 'Ajouter un ticket' }}
                </h3>

                <form @submit.prevent="handleSubmit" class="space-y-4">
                  <div>
                    <label for="first_name" class="block text-sm font-medium text-gray-700 mb-2">
                      Prénom <span class="text-red-500">*</span>
                    </label>
                    <UInput
                      id="first_name"
                      v-model="ticketForm.first_name"
                      placeholder="John"
                      :disabled="saving"
                      required
                      class="w-full"
                    />
                  </div>

                  <div>
                    <label for="last_name" class="block text-sm font-medium text-gray-700 mb-2">
                      Nom <span class="text-red-500">*</span>
                    </label>
                    <UInput
                      id="last_name"
                      v-model="ticketForm.last_name"
                      placeholder="Doe"
                      :disabled="saving"
                      required
                      class="w-full"
                    />
                  </div>

                  <AlertMessage
                    v-if="formError"
                    :message="formError"
                    type="error"
                    class="text-sm"
                  />

                  <div class="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                    <UButton
                      type="submit"
                      color="primary"
                      :loading="saving"
                      :disabled="saving"
                      class="w-full sm:col-start-2"
                    >
                      {{ editingTicket ? 'Modifier' : 'Ajouter' }}
                    </UButton>
                    <UButton
                      type="button"
                      color="neutral"
                      variant="outline"
                      @click="closeModal"
                      :disabled="saving"
                      class="w-full sm:col-start-1 mt-3 sm:mt-0"
                    >
                      Annuler
                    </UButton>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import type { TicketSchema, TicketDataSchema } from '~/utils/api';
import { getEventsPartnershipTickets, postEventsPartnershipTickets, putEventsPartnershipTickets } from '~/utils/api';

interface Props {
  eventSlug: string;
  partnershipId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  ticketsUpdated: [tickets: TicketSchema[]];
}>();

const toast = useToast();

const tickets = ref<TicketSchema[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

const isModalOpen = ref(false);
const editingTicket = ref<TicketSchema | null>(null);
const saving = ref(false);
const formError = ref<string | null>(null);

const ticketForm = ref<TicketDataSchema>({
  first_name: '',
  last_name: ''
});

async function loadTickets() {
  try {
    loading.value = true;
    error.value = null;

    console.log('Loading tickets for:', {
      eventSlug: props.eventSlug,
      partnershipId: props.partnershipId
    });

    const response = await getEventsPartnershipTickets(
      props.eventSlug,
      props.partnershipId
    );

    tickets.value = response;
  } catch (err) {
    console.error('Failed to load tickets:', err);
    console.error('Error details:', err);
    error.value = `Impossible de charger les tickets: ${err instanceof Error ? err.message : 'Erreur inconnue'}`;
  } finally {
    loading.value = false;
  }
}

function openAddTicketModal() {
  editingTicket.value = null;
  ticketForm.value = {
    first_name: '',
    last_name: ''
  };
  formError.value = null;
  isModalOpen.value = true;
}

function openEditTicketModal(ticket: TicketSchema) {
  editingTicket.value = ticket;
  ticketForm.value = {
    first_name: ticket.data.first_name,
    last_name: ticket.data.last_name
  };
  formError.value = null;
  isModalOpen.value = true;
}

function closeModal() {
  if (saving.value) return;
  isModalOpen.value = false;
  editingTicket.value = null;
  formError.value = null;
}

async function handleSubmit() {
  formError.value = null;

  // Validation
  if (!ticketForm.value.first_name.trim()) {
    formError.value = 'Le prénom est obligatoire';
    return;
  }

  if (!ticketForm.value.last_name.trim()) {
    formError.value = 'Le nom est obligatoire';
    return;
  }

  try {
    saving.value = true;

    if (editingTicket.value) {
      // Mise à jour d'un ticket existant
      await putEventsPartnershipTickets(
        props.eventSlug,
        props.partnershipId,
        editingTicket.value.id,
        ticketForm.value
      );

      toast.add({
        title: 'Succès',
        description: 'Le ticket a été modifié avec succès',
        color: 'success'
      });
    } else {
      // Création d'un nouveau ticket
      await postEventsPartnershipTickets(
        props.eventSlug,
        props.partnershipId,
        [ticketForm.value]
      );

      toast.add({
        title: 'Succès',
        description: 'Le ticket a été ajouté avec succès',
        color: 'success'
      });
    }

    // Recharger la liste des tickets
    await loadTickets();

    // Émettre l'événement
    emit('ticketsUpdated', tickets.value);

    // Fermer le modal
    closeModal();
  } catch (err) {
    console.error('Failed to save ticket:', err);

    formError.value = editingTicket.value
      ? 'Impossible de modifier le ticket'
      : 'Impossible d\'ajouter le ticket';

    toast.add({
      title: 'Erreur',
      description: formError.value,
      color: 'error'
    });
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadTickets();
});

// Exposer la méthode de rechargement pour l'utiliser depuis le parent si nécessaire
defineExpose({
  loadTickets
});
</script>
