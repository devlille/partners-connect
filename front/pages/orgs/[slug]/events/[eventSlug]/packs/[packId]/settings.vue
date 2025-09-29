<template>
  <div v-if="pack">
    <SponsoringPackForm 
      :data="packFormData" 
      @save="handleSave"
    />
  </div>
  
  <div v-else-if="loadingPack">
    Chargement...
  </div>
  
  <div v-else>
    Pack de sponsoring non trouvé
  </div>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugPacks, putOrgsOrgSlugEventsEventSlugPacksPackId, type SponsoringPack, type CreateSponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const router = useRouter();
const pack = ref<SponsoringPack | null>(null);
const loading = ref(false);
const loadingPack = ref(true);

const packFormData = computed(() => {
  if (!pack.value) return {};
  
  return {
    name: pack.value.name,
    price: pack.value.base_price,
    with_booth: pack.value.with_booth, 
    nb_tickets: pack.value.nb_tickets, // TODO: Map from existing pack data if available
    max_quantity: pack.value.max_quantity
  };
});

onMounted(async () => {
  try {
    const response = await getOrgsOrgSlugEventsEventSlugPacks(
      route.params.slug as string,
      route.params.eventSlug as string
    );
    
    // Find the specific pack by ID
    pack.value = response.data.find(p => p.id === route.params.packId) || null;
  } catch (error) {
    console.error('Failed to load sponsoring pack:', error);
  } finally {
    loadingPack.value = false;
  }
});

async function handleSave(formData: CreateSponsoringPack) {
  loading.value = true;
  
  try {
    await putOrgsOrgSlugEventsEventSlugPacksPackId(
      route.params.slug as string,
      route.params.eventSlug as string,
      route.params.packId as string,
      formData
    );
    
    // Redirect back to pack detail page
    await router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}/packs/${route.params.packId}`);
  } catch (error) {
    console.error('Failed to update sponsoring pack:', error);
    // TODO: Add error handling/notification
  } finally {
    loading.value = false;
  }
}

useHead({
  title: () => pack.value ? `Paramètres - ${pack.value.name}` : 'Paramètres du pack de sponsoring'
});
</script>