<template>
  <UCard>
    <form @submit.prevent="onSave" class="space-y-6">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-2">
          Nom de l'événement <span class="text-red-500">*</span>
        </label>
        <UInput
          id="name"
          v-model="form.name"
          placeholder="DevLille 2026"
          size="lg"
          class="w-full"
        />
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="start_time" class="block text-sm font-medium text-gray-700 mb-2">
            Début <span class="text-red-500">*</span>
          </label>
          <UInput id="start_time" v-model="form.start_time" type="datetime-local" class="w-full" />
        </div>

        <div>
          <label for="end_time" class="block text-sm font-medium text-gray-700 mb-2">
            Fin <span class="text-red-500">*</span>
          </label>
          <UInput id="end_time" v-model="form.end_time" type="datetime-local" class="w-full" />
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="submission_start_time" class="block text-sm font-medium text-gray-700 mb-2">
            Début des soumissions <span class="text-red-500">*</span>
          </label>
          <UInput
            id="submission_start_time"
            v-model="form.submission_start_time"
            type="datetime-local"
            class="w-full"
          />
        </div>

        <div>
          <label for="submission_end_time" class="block text-sm font-medium text-gray-700 mb-2">
            Fin des soumissions <span class="text-red-500">*</span>
          </label>
          <UInput
            id="submission_end_time"
            v-model="form.submission_end_time"
            type="datetime-local"
            class="w-full"
          />
        </div>
      </div>

      <div>
        <label for="address" class="block text-sm font-medium text-gray-700 mb-2">
          Adresse <span class="text-red-500">*</span>
        </label>
        <UInput
          id="address"
          v-model="form.address"
          placeholder="123 Rue de la Tech, Lille"
          icon="i-heroicons-map-pin"
          class="w-full"
        />
      </div>

      <UDivider label="Questions & Réponses" />

      <div class="flex items-center gap-3">
        <USwitch v-model="form.qanda_enabled" />
        <label class="text-sm font-medium text-gray-700">Activer les questions & réponses</label>
      </div>

      <div v-if="form.qanda_enabled" class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="qanda_max_questions" class="block text-sm font-medium text-gray-700 mb-2">
            Nombre max de questions
          </label>
          <UInput
            id="qanda_max_questions"
            :model-value="form.qanda_max_questions ?? ''"
            type="number"
            min="1"
            placeholder="Illimité"
            class="w-full"
            @update:model-value="val => form.qanda_max_questions = val === '' ? null : Number(val)"
          />
        </div>

        <div>
          <label for="qanda_max_answers" class="block text-sm font-medium text-gray-700 mb-2">
            Nombre max de réponses
          </label>
          <UInput
            id="qanda_max_answers"
            :model-value="form.qanda_max_answers ?? ''"
            type="number"
            min="2"
            placeholder="Illimité"
            class="w-full"
            @update:model-value="val => form.qanda_max_answers = val === '' ? null : Number(val)"
          />
        </div>
      </div>

      <UDivider label="Contact" />

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="contact_email" class="block text-sm font-medium text-gray-700 mb-2">
            Email <span class="text-red-500">*</span>
          </label>
          <UInput
            id="contact_email"
            v-model="form.contact.email"
            type="email"
            placeholder="contact@devlille.fr"
            icon="i-heroicons-envelope"
            class="w-full"
          />
        </div>

        <div>
          <label for="contact_phone" class="block text-sm font-medium text-gray-700 mb-2">
            Téléphone
          </label>
          <UInput
            id="contact_phone"
            v-model="form.contact.phone"
            type="tel"
            placeholder="+33 1 23 45 67 89"
            icon="i-heroicons-phone"
            class="w-full"
          />
        </div>
      </div>

      <div class="flex justify-end gap-3 pt-4">
        <UButton type="submit" size="lg" label="Valider" icon="i-heroicons-check" />
      </div>
    </form>
  </UCard>
</template>

<script lang="ts" setup>
const { data } = defineProps<{ data: Omit<EventDisplay, 'slug'>}>()

const emit = defineEmits<{
  (e: 'save', payload: Omit<EventDisplay, 'slug'>): void
}>()

type FormState = Omit<EventDisplay, 'slug' | 'qanda_config'> & {
  qanda_enabled: boolean
  qanda_max_questions: number | null
  qanda_max_answers: number | null
}

const form = ref<FormState>({
  ...data,
  qanda_enabled: !!data.qanda_config,
  qanda_max_questions: data.qanda_config?.max_questions ?? null,
  qanda_max_answers: data.qanda_config?.max_answers ?? null,
})

function onSave() {
  const { qanda_enabled, qanda_max_questions, qanda_max_answers, ...rest } = form.value
  emit('save', {
    ...rest,
    qanda_config: qanda_enabled
      ? { max_questions: qanda_max_questions, max_answers: qanda_max_answers }
      : null,
  })
}
</script>
