<template>
  <div class="space-y-6">
    <form @submit.prevent="handleSubmit" class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="name" class="block text-sm font-medium text-gray-700 mb-2">
            Nom de l'entreprise (optionnel)
          </label>
          <UInput
            id="name"
            v-model="form.name"
            placeholder="Nom de facturation"
            :disabled="readonly || loading"
            class="w-full"
          />
        </div>

        <div>
          <label for="po" class="block text-sm font-medium text-gray-700 mb-2">
            Bon de commande (optionnel)
          </label>
          <UInput
            id="po"
            v-model="form.po"
            placeholder="Numéro de bon de commande"
            :disabled="readonly || loading"
            class="w-full"
          />
        </div>
      </div>

      <div class="border-t pt-4">
        <h3 class="text-sm font-semibold text-gray-900 mb-4">Contact de facturation</h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label for="firstName" class="block text-sm font-medium text-gray-700 mb-2">
              Prénom <span class="text-red-500">*</span>
            </label>
            <UInput
              id="firstName"
              v-model="form.contact.first_name"
              placeholder="Prénom"
              required
              :disabled="readonly || loading"
              class="w-full"
            />
          </div>

          <div>
            <label for="lastName" class="block text-sm font-medium text-gray-700 mb-2">
              Nom <span class="text-red-500">*</span>
            </label>
            <UInput
              id="lastName"
              v-model="form.contact.last_name"
              placeholder="Nom"
              required
              :disabled="readonly || loading"
              class="w-full"
            />
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-gray-700 mb-2">
              Email <span class="text-red-500">*</span>
            </label>
            <UInput
              id="email"
              v-model="form.contact.email"
              type="email"
              placeholder="email@example.com"
              required
              :disabled="readonly || loading"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <div v-if="!readonly" class="flex gap-3 pt-4">
        <UButton
          type="submit"
          color="primary"
          :loading="loading"
          :disabled="!isFormValid"
        >
          Mettre à jour les informations de facturation
        </UButton>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import type { CompanyBillingData } from '~/utils/api';
import type { ExtendedPartnershipItem } from '~/types/partnership';

interface Props {
  partnership: ExtendedPartnershipItem;
  billing?: CompanyBillingData | null;
  readonly?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  billing: null,
  readonly: false,
  loading: false
});

const emit = defineEmits<{
  save: [data: CompanyBillingData];
}>();

const { form, isFormValid, resetForm, prepareBillingData } = useBillingForm(props.billing);

function handleSubmit() {
  const billingData = prepareBillingData();
  emit('save', billingData);
}

// Watch for props changes to update form
watch(() => props.billing, (newBilling) => {
  if (newBilling) {
    resetForm(newBilling);
  }
}, { deep: true });
</script>
