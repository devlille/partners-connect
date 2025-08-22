<template>
  <h2>{{ form.name }}</h2>

  <form @submit.prevent="submitForm">
    <p>
      <label for="name">Nom de l'événement</label>
      <input
        id="name"
        v-model="form.name"
        type="text"
        name="name"
        autocomplete="organization"
      />
    </p>

    <p>
      <label for="start_time">Début</label>
      <input
        id="start_time"
        v-model="form.start_time"
        type="datetime-local"
        name="start_time"
        step="1"
      />
    </p>

    <p>
      <label for="end_time">Fin</label>
      <input
        id="end_time"
        v-model="form.end_time"
        type="datetime-local"
        name="end_time"
        step="1"
      />
    </p>

    <p>
      <label for="submission_start_time">Début des soumissions</label>
      <input
        id="submission_start_time"
        v-model="form.submission_start_time"
        type="datetime-local"
        name="submission_start_time"
        step="1"
      />
    </p>

    <p>
      <label for="submission_end_time">Fin des soumissions</label>
      <input
        id="submission_end_time"
        v-model="form.submission_end_time"
        type="datetime-local"
        name="submission_end_time"
        step="1"
      />
    </p>

    <p>
      <label for="address">Adresse</label>
      <input
        id="address"
        v-model="form.address"
        type="text"
        name="address"
        autocomplete="street-address"
      />
    </p>

    <fieldset>
      <legend>Contact</legend>
      <p>
        <label for="contact_phone">Téléphone</label>
        <input
          id="contact_phone"
          v-model="form.contact.phone"
          type="tel"
          name="contact.phone"
          autocomplete="tel"
        />
      </p>
      <p>
        <label for="contact_email">Email</label>
        <input
          id="contact_email"
          v-model="form.contact.email"
          type="email"
          name="contact.email"
          autocomplete="email"
        />
      </p>
    </fieldset>

    <p class="buttons-bar">
      <input type="submit" value="Valider" />
    </p>
  </form>
</template>

<script setup lang="ts">
import { ref } from "vue";

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

async function submitForm() {
  await putOrgsOrgSlugEventsEventSlug(
    route.params.slug as string,
    route.params.id as string,
    {
      ...event,
      ...form.value,
    }
  );
}

definePageMeta({
  layout: "event",
});

useHead({
  title: "Organisation | DevLille",
  script: [
    { src: "/js/main-nav.js", type: "text/javascript", defer: true },
    { src: "/js/autoScroll.js", type: "text/javascript", defer: true },
    { src: "/js/tabs.js", type: "text/javascript", defer: true },
  ],
});
</script>
