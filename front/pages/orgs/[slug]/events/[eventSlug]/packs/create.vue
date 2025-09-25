<template>
  <SponsoringPackForm 
    :data="packData" 
    @save="handleSave"
  />
</template>

<script setup lang="ts">
import { postOrgsOrgSlugEventsEventSlugPacks, type CreateSponsoringPack } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const route = useRoute();
const router = useRouter();

const packData = ref({
  name: "",
  description: "",
  price: 0,
  max_quantity: undefined
});

async function handleSave(formData: CreateSponsoringPack) {
  try {
    const response = await postOrgsOrgSlugEventsEventSlugPacks(
      route.params.slug as string,
      route.params.eventSlug as string,
      formData
    );
    
    // Redirect to pack detail page
    await router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}/packs/${response.data.id}`);
  } catch (error) {
    console.error('Failed to create sponsoring pack:', error);
    // TODO: Add error handling/notification
  }
}

useHead({
  title: `Créer un pack de sponsoring | ${route.params.eventSlug}`
});
</script>