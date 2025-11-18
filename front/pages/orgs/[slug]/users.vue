<template>
  <Dashboard :main-links="orgLinks" :footer-links="footerLinks">
    <div class="bg-white border-b border-gray-200 p-6">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>Utilisateurs</PageTitle>
          <p class="mt-1 text-sm text-gray-500">
            Gérez les personnes ayant accès à l'organisation {{ orgSlug }}
          </p>
        </div>
        <UButton
          color="primary"
          size="lg"
          icon="i-heroicons-plus"
          @click="isAddModalOpen = true"
        >
          Ajouter des emails
        </UButton>
      </div>
    </div>

    <div class="p-6">
      <!-- Loading state -->
      <TableSkeleton v-if="loading" :columns="2" :rows="8" />

      <!-- Error state -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ error }}
      </div>

      <!-- Users list -->
      <div v-else class="bg-white rounded-lg shadow">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">
            Utilisateurs autorisés ({{ users.length }})
          </h2>
        </div>

        <div v-if="users.length === 0" class="px-6 py-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          <h3 class="mt-2 text-sm font-medium text-gray-900">Aucun utilisateur</h3>
          <p class="mt-1 text-sm text-gray-500">Commencez par ajouter des emails pour donner accès à l'organisation.</p>
        </div>

        <ul v-else class="divide-y divide-gray-200">
          <li v-for="user in users" :key="user.email" class="px-6 py-4 hover:bg-gray-50">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-4">
                <!-- Avatar avec image -->
                <div v-if="user.picture_url && user.picture_url.trim() !== ''" class="shrink-0">
                  <img
                    :src="user.picture_url"
                    :alt="user.display_name || user.email"
                    class="h-10 w-10 rounded-full object-cover"
                    @error="handleImageError($event, user)"
                  >
                </div>
                <!-- Avatar avec initiales -->
                <div v-else class="shrink-0">
                  <div class="h-10 w-10 rounded-full bg-primary-100 flex items-center justify-center">
                    <span class="text-primary-700 font-medium text-sm">
                      {{ getInitials(user.display_name || user.email) }}
                    </span>
                  </div>
                </div>
                <div>
                  <p class="text-sm font-medium text-gray-900">
                    {{ user.display_name || user.email }}
                  </p>
                  <p v-if="user.display_name" class="text-sm text-gray-500">
                    {{ user.email }}
                  </p>
                </div>
              </div>
              <UButton
                color="error"
                variant="ghost"
                size="sm"
                icon="i-heroicons-trash"
                :loading="deletingEmail === user.email"
                @click="confirmDelete(user)"
              >
                Supprimer
              </UButton>
            </div>
          </li>
        </ul>
      </div>
    </div>

    <!-- Modal d'ajout -->
    <Teleport to="body">
      <div v-if="isAddModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isAddModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Ajouter des accès</h3>
          </div>

          <div class="px-6 py-4">
            <form @submit.prevent="handleAddEmails" class="space-y-4">
              <div>
                <label for="emails" class="block text-sm font-medium text-gray-700 mb-2">
                  Adresses email (une par ligne)
                </label>
                <UTextarea
                  id="emails"
                  v-model="newEmails"
                  :rows="6"
                  placeholder="exemple@email.com&#10;autre@email.com"
                  :disabled="isSubmitting"
                  class="w-full"
                />
                <p class="mt-1 text-xs text-gray-500">
                  Entrez une adresse email par ligne
                </p>
              </div>

              <div v-if="addError" class="text-sm text-red-600">
                {{ addError }}
              </div>
            </form>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="isSubmitting"
              @click="isAddModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="primary"
              :loading="isSubmitting"
              @click="handleAddEmails"
            >
              Ajouter
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal de confirmation de suppression -->
    <Teleport to="body">
      <div v-if="isDeleteModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" @click.self="isDeleteModalOpen = false">
        <div class="w-full max-w-lg bg-white rounded-lg shadow-xl" @click.stop>
          <div class="px-6 py-4 border-b border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900">Confirmer la suppression</h3>
          </div>

          <div class="px-6 py-4 space-y-4">
            <p class="text-sm text-gray-700">
              Êtes-vous sûr de vouloir retirer l'accès à <strong>{{ userToDelete?.email }}</strong> ?
            </p>
            <p class="text-sm text-gray-500">
              Cette personne ne pourra plus accéder à l'organisation.
            </p>
          </div>

          <div class="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
            <UButton
              color="neutral"
              variant="ghost"
              :disabled="!!deletingEmail"
              @click="isDeleteModalOpen = false"
            >
              Annuler
            </UButton>
            <UButton
              color="error"
              :loading="!!deletingEmail"
              @click="handleDelete"
            >
              Supprimer
            </UButton>
          </div>
        </div>
      </div>
    </Teleport>
  </Dashboard>
</template>

<script setup lang="ts">
import { getOrgsUsers, postOrgsUsersGrant, type UserSchema } from '~/utils/api';
import authMiddleware from '~/middleware/auth';

const route = useRoute();
const { footerLinks } = useDashboardLinks();

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] as string : params as string;
});

const { orgLinks } = useOrgLinks(orgSlug.value);

const users = ref<UserSchema[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);

const isAddModalOpen = ref(false);
const newEmails = ref('');
const isSubmitting = ref(false);
const addError = ref<string | null>(null);

const isDeleteModalOpen = ref(false);
const userToDelete = ref<UserSchema | null>(null);
const deletingEmail = ref<string | null>(null);

async function loadUsers() {
  try {
    loading.value = true;
    error.value = null;
    const response = await getOrgsUsers(orgSlug.value);
    users.value = response.data;
  } catch (err) {
    console.error('Failed to load users:', err);
    error.value = 'Impossible de charger la liste des utilisateurs';
  } finally {
    loading.value = false;
  }
}

function getInitials(name: string): string {
  return name
    .split(/\s+/)
    .map(part => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

function handleImageError(_event: Event, user: UserSchema) {
  console.log('Image load error for user:', user.email, 'URL:', user.picture_url);
  // Cacher l'image en cas d'erreur en mettant picture_url à null
  const userIndex = users.value.findIndex(u => u.email === user.email);
  if (userIndex !== -1) {
    users.value[userIndex] = { ...user, picture_url: null };
  }
}

async function handleAddEmails() {
  addError.value = null;

  const emailLines = newEmails.value
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);

  if (emailLines.length === 0) {
    addError.value = 'Veuillez entrer au moins une adresse email';
    return;
  }

  // Validation basique des emails
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const invalidEmails = emailLines.filter(email => !emailRegex.test(email));

  if (invalidEmails.length > 0) {
    addError.value = `Emails invalides : ${invalidEmails.join(', ')}`;
    return;
  }

  try {
    isSubmitting.value = true;

    // Récupérer tous les emails existants
    const existingEmails = users.value.map(user => user.email);

    // Combiner les emails existants avec les nouveaux (sans doublons)
    const allEmails = [...new Set([...existingEmails, ...emailLines])];

    // Envoyer tous les emails (existants + nouveaux)
    await postOrgsUsersGrant(orgSlug.value, { user_emails: allEmails });

    // Recharger la liste
    await loadUsers();

    // Fermer le modal et réinitialiser
    isAddModalOpen.value = false;
    newEmails.value = '';
  } catch (err: any) {
    console.error('Failed to add emails:', err);
    console.error('Error response:', err.response);
    console.error('Error data:', err.response?.data);

    // Extraire le message d'erreur du serveur
    const serverMessage = err.response?.data?.message || '';
    console.log('Server message:', serverMessage);

    // Détecter si c'est une erreur d'utilisateur non trouvé
    const notFoundMatch = serverMessage.match(/User with email: (.+?) not found/);
    console.log('Not found match:', notFoundMatch);

    if (notFoundMatch) {
      const unknownEmail = notFoundMatch[1];
      addError.value = `L'adresse email "${unknownEmail}" n'est pas enregistrée dans le système. L'utilisateur doit d'abord se connecter au moins une fois avant de pouvoir être ajouté à une organisation.`;
    } else if (serverMessage) {
      addError.value = serverMessage;
    } else {
      addError.value = 'Impossible d\'ajouter les emails. Vérifiez les informations.';
    }

    console.log('Final error message:', addError.value);
  } finally {
    isSubmitting.value = false;
  }
}

function confirmDelete(user: UserSchema) {
  userToDelete.value = user;
  isDeleteModalOpen.value = true;
}

async function handleDelete() {
  if (!userToDelete.value) return;

  try {
    deletingEmail.value = userToDelete.value.email;

    // Récupérer tous les emails existants sauf celui à supprimer
    const remainingEmails = users.value
      .filter(user => user.email !== userToDelete.value?.email)
      .map(user => user.email);

    // Envoyer la liste mise à jour (sans l'email supprimé)
    await postOrgsUsersGrant(orgSlug.value, { user_emails: remainingEmails });

    // Recharger la liste
    await loadUsers();

    // Fermer le modal
    isDeleteModalOpen.value = false;
    userToDelete.value = null;
  } catch (err) {
    console.error('Failed to delete user:', err);
    error.value = 'Impossible de supprimer l\'utilisateur';
  } finally {
    deletingEmail.value = null;
  }
}

onMounted(() => {
  loadUsers();
});

useHead({
  title: computed(() => `Gestion des accès - ${orgSlug.value} | DevLille`)
});
</script>
