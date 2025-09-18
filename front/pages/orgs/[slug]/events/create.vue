<template>
  <h2>{{ form.name }}</h2>

  <event-form :data="form" @save="submitForm" />
</template>

<script setup lang="ts">
import { ref } from "vue";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const router = useRouter()

const form = ref({
  name: "",
  start_time: "",
  end_time: "",
  submission_start_time: "",
  submission_end_time: "",
  address: "",
  contact: {
    phone: "",
    email: "",
  },
});

async function submitForm(event: Omit<EventDisplay, 'slug'>) {
  await postOrgsOrgSlugEvents(
    route.params.slug as string,
    event
  ).then(response => router.push(`/orgs/${route.params.slug}/events/${response.data.slug}/settings`))
}

definePageMeta({
  layout: "event",
  middleware: authMiddleware,
  ssr: false
});

useHead({
  title: "Organisation | DevLille"
});
</script>
