<template>
  <div v-if="event">
    <EventForm 
      :data="eventFormData" 
      @save="handleSave"
      @cancel="goBack"
      :loading="loading"
      submit-text="Mettre à jour l'événement"
      loading-text="Mise à jour..."
    />
  </div>
  
  <div v-else-if="loadingEvent">
    Chargement...
  </div>
  
  <div v-else>
    Événement non trouvé
  </div>
</template>

<script setup lang="ts">
import { getEventsEventSlug, putOrgsOrgSlugEventsEventSlug, type GetEventsEventSlugResult } from "~/utils/api";
import authMiddleware from "~/middleware/auth";

definePageMeta({
  middleware: authMiddleware,
  ssr: false
})

const route = useRoute();
const router = useRouter();
const event = ref<GetEventsEventSlugResult['data'] | null>(null);
const loading = ref(false);
const loadingEvent = ref(true);

const eventFormData = computed(() => {
  if (!event.value) return {};
  
  return {
    name: event.value.event.name,
    start_time: event.value.event.start_time,
    end_time: event.value.event.end_time,
    submission_start_time: event.value.event.submission_start_time,
    submission_end_time: event.value.event.submission_end_time,
    address: event.value.event.address,
    contact: {
      email: event.value.event.contact.email,
      phone: event.value.event.contact.phone || ""
    }
  };
});

onMounted(async () => {
  try {
    const response = await getEventsEventSlug(route.params.eventSlug as string);
    event.value = response.data;
  } catch (error) {
    console.error('Failed to load event:', error);
  } finally {
    loadingEvent.value = false;
  }
});

async function handleSave(formData: any) {
  loading.value = true;
  
  try {
    // Prepare payload for API
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

    await putOrgsOrgSlugEventsEventSlug(
      route.params.slug as string, 
      route.params.eventSlug as string, 
      payload
    );
    
    // Redirect back to event detail page
    await router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}`);
  } catch (error) {
    console.error('Failed to update event:', error);
    // TODO: Add error handling/notification
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push(`/orgs/${route.params.slug}/events/${route.params.eventSlug}`);
}

useHead({
  title: () => event.value ? `Paramètres - ${event.value.event.name}` : 'Paramètres de l\'événement'
});
</script>