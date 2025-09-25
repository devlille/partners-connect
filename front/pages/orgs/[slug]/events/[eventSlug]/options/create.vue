<template>
  <SponsoringOptionForm 
    :data="optionData" 
    @save="handleSave"
  />
</template>

<script setup lang="ts">
import { postOrgsOrgSlugEventsEventSlugOptions, type CreateSponsoringOption } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const route = useRoute();
const router = useRouter();

const optionData = ref({
  name: "",
  description: "",
  price: undefined
});

async function handleSave(formData: CreateSponsoringOption) {
  try {
    const response = await postOrgsOrgSlugEventsEventSlugOptions(
      route.params.slug as string,
      route.params.eventSlug as string,
      formData
    );
    
    // Redirect to option detail page
    await router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}/options/${response.data.id}`);
  } catch (error) {
    console.error('Failed to create sponsoring option:', error);
    // TODO: Add error handling/notification
  }
}

useHead({
  title: `Créer une option de sponsoring | ${route.params.eventSlug}`
});
</script>