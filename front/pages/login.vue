<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-primary-100 dark:from-gray-900 dark:to-gray-800">
    <UCard class="w-full max-w-md">
      <template #header>
        <div class="text-center">
          <h1 class="text-3xl font-bold text-gray-900 dark:text-white mb-2">
            Connexion
          </h1>
          <p class="text-gray-600 dark:text-gray-400">
            Espace partenaires
          </p>
        </div>
      </template>

      <div class="space-y-6">
        <div class="text-center">
          <p class="text-gray-700 dark:text-gray-300 mb-6">
            Connectez-vous avec votre compte Google pour accéder à l'espace partenaires.
          </p>

          <UButton
            color="white"
            size="xl"
            class="w-full"
            :loading="isLoading"
            @click="handleLogin"
          >
            <template #leading>
              <svg class="w-5 h-5" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="currentColor"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="currentColor"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                />
                <path
                  fill="currentColor"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                />
              </svg>
            </template>
            Se connecter avec Google
          </UButton>
        </div>

        <UDivider label="OU" />

        <div class="text-center">
          <UButton
            to="/"
            color="gray"
            variant="ghost"
            size="lg"
          >
            <template #leading>
              <UIcon name="i-heroicons-arrow-left" />
            </template>
            Retour à l'accueil
          </UButton>
        </div>
      </div>

      <template #footer>
        <div class="text-center text-sm text-gray-500 dark:text-gray-400">
          <p>En vous connectant, vous acceptez nos conditions d'utilisation.</p>
        </div>
      </template>
    </UCard>
  </div>
</template>

<script setup lang="ts">
const { login } = useAuth();
const isLoading = ref(false);

const handleLogin = async () => {
  try {
    isLoading.value = true;
    await login();
  } catch (error) {
    console.error('Login failed:', error);
  } finally {
    isLoading.value = false;
  }
};

definePageMeta({
  layout: 'default'
});

useHead({
  title: 'Connexion | DevLille'
});
</script>