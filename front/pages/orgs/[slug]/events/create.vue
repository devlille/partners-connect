<template>
  <EventForm 
    title="Créer un nouvel événement"
    :data="eventData" 
    @save="handleSave"
    @cancel="goBack"
    :loading="loading"
    submit-text="Créer l'événement"
    loading-text="Création..."
  />
</template>

<script setup lang="ts">
import { postOrgsOrgSlugEvents, type PostOrgsOrgSlugEventsResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
});

const route = useRoute();
const router = useRouter();
const loading = ref(false);

const eventData = ref({
  name: "",
  start_time: "",
  end_time: "",
  submission_start_time: "",
  submission_end_time: "",
  address: "",
  contact: {
    email: "",
    phone: ""
  }
});

async function handleSave(formData: any) {
  loading.value = true;
  
  try {
    // Filter out empty properties
    const payload = {
      name: formData.name,
      start_time: formData.start_time,
      end_time: formData.end_time,
      submission_start_time: formData.submission_start_time || undefined,
      submission_end_time: formData.submission_end_time || undefined,
      address: formData.address,
      contact: {
        email: formData.contact.email,
        ...(formData.contact.phone && { phone: formData.contact.phone })
      }
    };
    
    const response = await postOrgsOrgSlugEvents(route.params.slug as string, payload);
    
    // Redirect to event detail page
    await router.push(`/orgs/${route.params.slug}/events/${response.data.slug}`);
  } catch (error) {
    console.error('Failed to create event:', error);
    // TODO: Add error handling/notification
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.back();
}

useHead({
  title: `Créer un événement | ${route.params.slug}`
});
</script>

