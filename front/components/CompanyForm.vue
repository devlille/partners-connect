<template>
  <div class="space-y-6">
    <AlertMessage v-if="error" type="error" :message="error" />

    <AlertMessage v-if="success" type="success" :message="success" />

    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">
          Nom de l'entreprise
        </label>
        <UInput
          id="name"
          v-model="form.name"
          placeholder="Nom de l'entreprise"
          :disabled="isLoading"
        />
      </div>

      <SiretInput
        v-model="form.siret"
        :disabled="isLoading"
      />

      <div>
        <label for="vat" class="block text-sm font-medium text-gray-700 mb-1">
          TVA
        </label>
        <UInput
          id="vat"
          v-model="form.vat"
          placeholder="FR00000000000"
          :disabled="isLoading"
        />
      </div>

      <UrlInput
        v-model="form.site_url"
        label="Site web"
        :disabled="isLoading"
      />

      <div>
        <label for="description" class="block text-sm font-medium text-gray-700 mb-1">
          Description
        </label>
        <textarea
          id="description"
          v-model="form.description"
          rows="4"
          class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          placeholder="Description de l'entreprise"
          :disabled="isLoading"
        />
      </div>

      <div class="border-t pt-4">
        <h3 class="text-sm font-semibold text-gray-900 mb-3">Adresse du siège social</h3>

        <div class="space-y-4">
          <div>
            <label for="address" class="block text-sm font-medium text-gray-700 mb-1">
              Adresse
            </label>
            <UInput
              id="address"
              v-model="form.head_office.address"
              placeholder="Rue, numéro..."
              :disabled="isLoading"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label for="zipCode" class="block text-sm font-medium text-gray-700 mb-1">
                Code postal
              </label>
              <UInput
                id="zipCode"
                v-model="form.head_office.zip_code"
                placeholder="00000"
                :disabled="isLoading"
              />
            </div>

            <div>
              <label for="city" class="block text-sm font-medium text-gray-700 mb-1">
                Ville
              </label>
              <UInput
                id="city"
                v-model="form.head_office.city"
                placeholder="Ville"
                :disabled="isLoading"
              />
            </div>
          </div>

          <div>
            <label for="country" class="block text-sm font-medium text-gray-700 mb-1">
              Pays
            </label>
            <UInput
              id="country"
              v-model="form.head_office.country"
              placeholder="France"
              :disabled="isLoading"
            />
          </div>
        </div>
      </div>

      <div class="flex gap-3 pt-4">
        <UButton
          type="submit"
          color="primary"
          :loading="isLoading"
        >
          Mettre à jour les informations
        </UButton>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { putCompanyById } from '~/utils/api';
import type { UpdateCompanySchema, CompanySchema, AddressSchema } from '~/utils/api';

const props = defineProps<{
  company: CompanySchema;
}>();

const emit = defineEmits<{
  saved: [];
}>();

const form = ref<UpdateCompanySchema>({
  name: props.company.name,
  siret: props.company.siret,
  vat: props.company.vat,
  site_url: props.company.site_url,
  description: props.company.description || '',
  head_office: {
    address: props.company.head_office.address,
    city: props.company.head_office.city,
    zip_code: props.company.head_office.zip_code,
    country: props.company.head_office.country
  }
});

const isLoading = ref(false);
const error = ref<string | null>(null);
const success = ref<string | null>(null);

async function handleSubmit() {
  error.value = null;
  success.value = null;
  isLoading.value = true;

  try {
    const updateData: UpdateCompanySchema = {
      name: form.value.name?.trim() || null,
      siret: form.value.siret?.trim() || null,
      vat: form.value.vat?.trim() || null,
      site_url: form.value.site_url?.trim() || null,
      description: form.value.description?.trim() || null,
      head_office: {
        address: form.value.head_office?.address?.trim() || '',
        city: form.value.head_office?.city?.trim() || '',
        zip_code: form.value.head_office?.zip_code?.trim() || '',
        country: form.value.head_office?.country?.trim() || ''
      }
    };

    await putCompanyById(props.company.id, updateData);
    success.value = 'Informations de l\'entreprise mises à jour avec succès';

    emit('saved');

    // Effacer le message de succès après 3 secondes
    setTimeout(() => {
      success.value = null;
    }, 3000);
  } catch (err: any) {
    console.error('Failed to update company:', err);
    error.value = err.response?.data?.message || 'Impossible de mettre à jour les informations de l\'entreprise';
  } finally {
    isLoading.value = false;
  }
}

// Mettre à jour le formulaire si les props changent
watch(() => props.company, (newCompany) => {
  form.value = {
    name: newCompany.name,
    siret: newCompany.siret,
    vat: newCompany.vat,
    site_url: newCompany.site_url,
    description: newCompany.description || '',
    head_office: {
      address: newCompany.head_office.address,
      city: newCompany.head_office.city,
      zip_code: newCompany.head_office.zip_code,
      country: newCompany.head_office.country
    }
  };
}, { deep: true });
</script>
