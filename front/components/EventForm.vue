<template>
  <UCard>
    <form @submit.prevent="onSave" class="space-y-4">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">
          Nom de l'événement <span class="text-red-500">*</span>
        </label>
        <UInput
          id="name"
          v-model="form.name"
          placeholder="DevLille 2026"
          size="lg"
        />
      </div>

      <div>
        <label for="start_time" class="block text-sm font-medium text-gray-700 mb-1">
          Début <span class="text-red-500">*</span>
        </label>
        <UInput
          id="start_time"
          v-model="form.start_time"
          type="datetime-local"
        />
      </div>

      <div>
        <label for="end_time" class="block text-sm font-medium text-gray-700 mb-1">
          Fin <span class="text-red-500">*</span>
        </label>
        <UInput
          id="end_time"
          v-model="form.end_time"
          type="datetime-local"
        />
      </div>

      <div>
        <label for="submission_start_time" class="block text-sm font-medium text-gray-700 mb-1">
          Début des soumissions <span class="text-red-500">*</span>
        </label>
        <UInput
          id="submission_start_time"
          v-model="form.submission_start_time"
          type="datetime-local"
        />
      </div>

      <div>
        <label for="submission_end_time" class="block text-sm font-medium text-gray-700 mb-1">
          Fin des soumissions <span class="text-red-500">*</span>
        </label>
        <UInput
          id="submission_end_time"
          v-model="form.submission_end_time"
          type="datetime-local"
        />
      </div>

      <div>
        <label for="address" class="block text-sm font-medium text-gray-700 mb-1">
          Adresse <span class="text-red-500">*</span>
        </label>
        <UInput
          id="address"
          v-model="form.address"
          placeholder="123 Rue de la Tech, Lille"
          icon="i-heroicons-map-pin"
        />
      </div>

      <UDivider label="Contact" />

      <div>
        <label for="contact_email" class="block text-sm font-medium text-gray-700 mb-1">
          Email <span class="text-red-500">*</span>
        </label>
        <UInput
          id="contact_email"
          v-model="form.contact.email"
          type="email"
          placeholder="contact@devlille.fr"
          icon="i-heroicons-envelope"
        />
      </div>

      <div>
        <label for="contact_phone" class="block text-sm font-medium text-gray-700 mb-1">
          Téléphone
        </label>
        <UInput
          id="contact_phone"
          v-model="form.contact.phone"
          type="tel"
          placeholder="+33 1 23 45 67 89"
          icon="i-heroicons-phone"
        />
      </div>

      <div class="flex justify-end gap-3 pt-4">
        <UButton
          type="submit"
          size="lg"
          label="Valider"
          icon="i-heroicons-check"
        />
      </div>
    </form>
  </UCard>
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

