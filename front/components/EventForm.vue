<template>
  <form @submit.prevent="onSave">
    <p>
      <label for="name">Nom de l'événement</label>
      <input
        id="name"
        v-model="form.name"
        type="text"
        name="name"
        autocomplete="organization"
      >
    </p>

    <p>
      <label for="start_time">Début</label>
      <input
        id="start_time"
        v-model="form.start_time"
        type="datetime-local"
        name="start_time"
        step="1"
      >
    </p>

    <p>
      <label for="end_time">Fin</label>
      <input
        id="end_time"
        v-model="form.end_time"
        type="datetime-local"
        name="end_time"
        step="1"
      >
    </p>

    <p>
      <label for="submission_start_time">Début des soumissions</label>
      <input
        id="submission_start_time"
        v-model="form.submission_start_time"
        type="datetime-local"
        name="submission_start_time"
        step="1"
      >
    </p>

    <p>
      <label for="submission_end_time">Fin des soumissions</label>
      <input
        id="submission_end_time"
        v-model="form.submission_end_time"
        type="datetime-local"
        name="submission_end_time"
        step="1"
      >
    </p>

    <p>
      <label for="address">Adresse</label>
      <input
        id="address"
        v-model="form.address"
        type="text"
        name="address"
        autocomplete="street-address"
      >
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
        >
      </p>
      <p>
        <label for="contact_email">Email</label>
        <input
          id="contact_email"
          v-model="form.contact.email"
          type="email"
          name="contact.email"
          autocomplete="email"
        >
      </p>
    </fieldset>

    <p class="buttons-bar">
      <input type="submit" value="Valider" >
    </p>
  </form>
</template>

<script lang="ts" setup>
const { data } = defineProps<{ data: Omit<EventDisplay, 'slug'>}>()

const emit = defineEmits<{
  (e: 'save', payload: Omit<EventDisplay, 'slug'>): void
}>()
const form = ref<Omit<EventDisplay, 'slug'>>(data)

function onSave() {
  emit('save', form.value)
}
</script>

