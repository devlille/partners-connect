<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-lg font-semibold text-gray-900">
        Activités ({{ state.activities.length }})
      </h2>
      <UButton
        color="primary"
        icon="i-heroicons-plus"
        @click="dispatch({ type: 'OPEN_CREATE_MODAL' })"
      >
        Ajouter une activité
      </UButton>
    </div>

    <div v-if="state.loading" role="status" aria-live="polite">
      <TableSkeleton :columns="3" :rows="4" />
    </div>

    <div
      v-else-if="state.error"
      class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
      role="alert"
    >
      {{ state.error }}
    </div>

    <div v-else-if="state.activities.length === 0" class="text-center py-12">
      <i class="i-heroicons-calendar-days text-gray-400 text-5xl mx-auto block mb-4" aria-hidden="true" />
      <h3 class="text-sm font-medium text-gray-900">Aucune activité</h3>
      <p class="mt-1 text-sm text-gray-500">Ajoutez des activités prévues sur votre stand.</p>
    </div>

    <ul v-else class="divide-y divide-gray-200">
      <li
        v-for="activity in state.activities"
        :key="activity.id"
        class="py-4 flex items-start justify-between gap-4"
      >
        <div class="flex-1">
          <h3 class="text-base font-semibold text-gray-900">{{ activity.title }}</h3>
          <p class="mt-1 text-sm text-gray-600">{{ activity.description }}</p>
          <div v-if="activity.start_time || activity.end_time" class="mt-2 flex gap-4 text-xs text-gray-500">
            <span v-if="activity.start_time">
              <span class="font-medium">Début :</span> {{ formatDateTime(activity.start_time) }}
            </span>
            <span v-if="activity.end_time">
              <span class="font-medium">Fin :</span> {{ formatDateTime(activity.end_time) }}
            </span>
          </div>
        </div>
        <div class="flex gap-2 shrink-0">
          <UButton
            color="neutral"
            variant="ghost"
            size="sm"
            icon="i-heroicons-pencil"
            :aria-label="`Modifier l'activité ${activity.title}`"
            @click="dispatch({ type: 'OPEN_EDIT_MODAL', payload: activity })"
          />
          <UButton
            color="error"
            variant="ghost"
            size="sm"
            icon="i-heroicons-trash"
            :loading="state.deletingId === activity.id"
            :aria-label="`Supprimer l'activité ${activity.title}`"
            @click="handleDelete(activity.id)"
          />
        </div>
      </li>
    </ul>

    <Teleport to="body">
      <div
        v-if="state.isModalOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
        role="dialog"
        aria-modal="true"
        @click.self="dispatch({ type: 'CLOSE_MODAL' })"
      >
        <div
          class="w-full max-w-lg bg-white rounded-lg shadow-xl"
          @click.stop
        >
          <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
            <h3 class="text-lg font-semibold text-gray-900">
              {{ state.editingActivity ? 'Modifier l\'activité' : 'Ajouter une activité' }}
            </h3>
            <UButton
              icon="i-heroicons-x-mark"
              color="neutral"
              variant="ghost"
              size="sm"
              aria-label="Fermer"
              @click="dispatch({ type: 'CLOSE_MODAL' })"
            />
          </div>

          <div class="px-6 py-4 space-y-4">
            <div>
              <label for="activity-title" class="block text-sm font-medium text-gray-700 mb-1">
                Titre <span class="text-red-500">*</span>
              </label>
              <UInput
                id="activity-title"
                :model-value="state.form.title"
                placeholder="Ex: Démo produit"
                :disabled="state.isSubmitting"
                class="w-full"
                @update:model-value="dispatch({ type: 'UPDATE_FORM', payload: { title: $event } })"
              />
            </div>

            <div>
              <label for="activity-description" class="block text-sm font-medium text-gray-700 mb-1">
                Description <span class="text-red-500">*</span>
              </label>
              <UTextarea
                id="activity-description"
                :model-value="state.form.description"
                placeholder="Décrivez votre activité..."
                :rows="3"
                :disabled="state.isSubmitting"
                class="w-full"
                @update:model-value="dispatch({ type: 'UPDATE_FORM', payload: { description: $event } })"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label for="activity-start" class="block text-sm font-medium text-gray-700 mb-1">
                  Heure de début (optionnel)
                </label>
                <UInput
                  id="activity-start"
                  :model-value="state.form.start_time ?? ''"
                  type="datetime-local"
                  :disabled="state.isSubmitting"
                  class="w-full"
                  @update:model-value="dispatch({ type: 'UPDATE_FORM', payload: { start_time: $event || null } })"
                />
              </div>
              <div>
                <label for="activity-end" class="block text-sm font-medium text-gray-700 mb-1">
                  Heure de fin (optionnel)
                </label>
                <UInput
                  id="activity-end"
                  :model-value="state.form.end_time ?? ''"
                  type="datetime-local"
                  :disabled="state.isSubmitting"
                  class="w-full"
                  @update:model-value="dispatch({ type: 'UPDATE_FORM', payload: { end_time: $event || null } })"
                />
              </div>
            </div>

            <p v-if="formError" class="text-sm text-red-600" role="alert">{{ formError }}</p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="state.isSubmitting"
              @click="dispatch({ type: 'CLOSE_MODAL' })"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="state.isSubmitting"
              @click="handleSubmit"
            >
              {{ state.editingActivity ? 'Enregistrer' : 'Ajouter' }}
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import {
  listBoothActivities,
  createBoothActivity,
  updateBoothActivity,
  deleteBoothActivity,
} from '~/utils/api';
import {
  activitiesReducer,
  initialActivitiesState,
  type ActivitiesAction,
} from './ActivitiesManager.reducer';

const props = defineProps<Readonly<{
  eventSlug: string;
  partnershipId: string;
}>>();

const state = ref({ ...initialActivitiesState });

function dispatch(action: ActivitiesAction) {
  state.value = activitiesReducer(state.value, action);
}

const formError = ref<string | null>(null);

async function loadActivities() {
  dispatch({ type: 'FETCH_START' });
  try {
    const response = await listBoothActivities(props.eventSlug, props.partnershipId);
    dispatch({ type: 'FETCH_SUCCESS', payload: response.data });
  } catch {
    dispatch({ type: 'FETCH_ERROR', payload: 'Impossible de charger les activités' });
  }
}

async function handleSubmit() {
  formError.value = null;
  const { title, description } = state.value.form;
  if (!title.trim()) {
    formError.value = 'Le titre est obligatoire';
    return;
  }
  if (!description.trim()) {
    formError.value = 'La description est obligatoire';
    return;
  }

  dispatch({ type: 'SET_SUBMITTING', payload: true });
  try {
    if (state.value.editingActivity) {
      await updateBoothActivity(
        props.eventSlug,
        props.partnershipId,
        state.value.editingActivity.id,
        state.value.form,
      );
    } else {
      await createBoothActivity(props.eventSlug, props.partnershipId, state.value.form);
    }
    dispatch({ type: 'CLOSE_MODAL' });
    await loadActivities();
  } catch {
    formError.value = 'Une erreur est survenue. Veuillez réessayer.';
    dispatch({ type: 'SET_SUBMITTING', payload: false });
  }
}

async function handleDelete(activityId: string) {
  dispatch({ type: 'SET_DELETING_ID', payload: activityId });
  try {
    await deleteBoothActivity(props.eventSlug, props.partnershipId, activityId);
    await loadActivities();
  } catch {
    dispatch({ type: 'FETCH_ERROR', payload: 'Impossible de supprimer l\'activité' });
  } finally {
    dispatch({ type: 'SET_DELETING_ID', payload: null });
  }
}

function formatDateTime(dateString: string): string {
  return new Date(dateString).toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

onMounted(loadActivities);
</script>
