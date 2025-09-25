<template>
  <div v-if="option">
    <SponsoringOptionForm 
      :data="optionFormData" 
      @save="handleSave"
      :loading="loading"
    />
  </div>
  
  <div v-else-if="loadingOption">
    Chargement...
  </div>
  
  <div v-else>
    Option de sponsoring non trouvée
  </div>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugOptions, putOrgsOrgSlugEventsEventSlugOptionsOptionId, type CreateSponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const router = useRouter();
const option = ref<any>(null);
const loading = ref(false);
const loadingOption = ref(true);

const optionFormData = computed(() => {
  if (!option.value) return {};
  
  return {
    name: option.value.name,
    description: option.value.description,
    price: option.value.price
  };
});

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugOptions(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    
    // Find the specific option by ID
    option.value = response.data.find(o => o.id === route.params.optionId) || null;
  } catch (error) {
    console.error('Failed to load sponsoring option:', error);
  } finally {
    loadingOption.value = false;
  }
});

async function handleSave(formData: CreateSponsoringOption) {
  loading.value = true;
  
  try {
    await putOrgsOrgSlugEventsEventSlugOptionsOptionId(
      route.params.slug as string,
      route.params.eventSlug as string,
      route.params.optionId as string,
      formData
    );
    
    // Redirect back to option detail page
    await router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}/options/${route.params.optionId}`);
  } catch (error) {
    console.error('Failed to update sponsoring option:', error);
    // TODO: Add error handling/notification
  } finally {
    loading.value = false;
  }
}

useHead({
  title: () => option.value ? `Paramètres - ${option.value.name}` : 'Paramètres de l\'option de sponsoring'
});
</script>