<template>
  <h2>{{ form.name }}</h2>

  <event-form :data="form" @save="submitForm" />
</template>

<script setup lang="ts">
import { ref } from "vue";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const event = await getEventsEventSlug(route.params.id as string).then(
  (r) => r.data.event
);

const form = ref({
  name: event?.name ?? "",
  start_time: event?.start_time ?? "",
  end_time: event?.end_time ?? "",
  submission_start_time: event?.submission_start_time ?? "",
  submission_end_time: event?.submission_end_time ?? "",
  address: event?.address ?? "",
  contact: {
    phone: event?.contact?.phone ?? "",
    email: event?.contact?.email ?? "",
  },
});

async function submitForm(event: Omit<EventDisplay, 'slug'>) {
  await putOrgsOrgSlugEventsEventSlug(
    route.params.slug as string,
    route.params.id as string,
    event
  );
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
