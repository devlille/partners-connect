<template>
  <form @submit.prevent="onSubmit" class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nom de la compagnie
        </label>
        <UInput
          v-model="form.company_name"
          placeholder="Nom de la compagnie"
          disabled
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nom de l'événement
        </label>
        <UInput
          v-model="form.event_name"
          placeholder="Nom de l'événement"
          disabled
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Nom du contact
        </label>
        <UInput
          v-model="form.contact_name"
          placeholder="Nom du contact"
          required
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Rôle du contact
        </label>
        <UInput
          v-model="form.contact_role"
          placeholder="Rôle"
          required
          class="w-full"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Pack
        </label>
        <UInput
          v-model="form.pack_name"
          placeholder="Pack de sponsoring"
          disabled
          class="w-full"
        />
      </div>

      <div>
        <LanguageSelect
          v-model="form.language"
          label="Langue"
        />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Email(s)
        </label>
        <UInput
          v-model="form.emails"
          placeholder="email@example.com"
          type="email"
          class="w-full"
        />
        <p class="text-xs text-gray-500 mt-1">Séparer plusieurs emails par des virgules</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Téléphone
        </label>
        <UInput
          v-model="form.phone"
          placeholder="+33 6 12 34 56 78"
          type="tel"
          class="w-full"
        />
      </div>
    </div>

    <div class="flex justify-end gap-3 pt-4 ">
      <UButton
        type="button"
        color="neutral"
        variant="ghost"
        label="Annuler"
        @click="$emit('cancel')"
      />
      <UButton
        type="submit"
        color="primary"
        label="Enregistrer"
        :loading="loading"
      />
    </div>
  </form>
</template>

<script setup lang="ts">
import type { PartnershipItem } from "~/utils/api";

interface Props {
  partnership?: PartnershipItem | null;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<{
  save: [data: any];
  cancel: [];
}>();

const form = ref({
  company_name: props.partnership?.company_name || '',
  event_name: props.partnership?.event_name || '',
  contact_name: props.partnership?.contact.display_name || '',
  contact_role: props.partnership?.contact.role || '',
  pack_name: props.partnership?.pack_name || props.partnership?.suggested_pack_name || '',
  language: props.partnership?.language || 'fr',
  emails: props.partnership?.emails || '',
  phone: props.partnership?.phone || ''
});

// Mettre à jour le formulaire si les props changent
watch(() => props.partnership, (newPartnership) => {
  if (newPartnership) {
    form.value = {
      company_name: newPartnership.company_name,
      event_name: newPartnership.event_name,
      contact_name: newPartnership.contact.display_name,
      contact_role: newPartnership.contact.role,
      pack_name: newPartnership.pack_name || newPartnership.suggested_pack_name || '',
      language: newPartnership.language,
      emails: newPartnership.emails || '',
      phone: newPartnership.phone || ''
    };
  }
}, { deep: true });

function onSubmit() {
  emit('save', {
    contact_name: form.value.contact_name,
    contact_role: form.value.contact_role,
    language: form.value.language,
    emails: form.value.emails ? form.value.emails.split(',').map(e => e.trim()) : [],
    phone: form.value.phone || null
  });
}
</script>