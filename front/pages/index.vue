<template>
  <header>
    <MainTitle> Demande de Partenariat </MainTitle>
  </header>

  <div id="container">
    <main>
      <div v-if="error" class="flex items-center justify-center min-h-[400px]">
        <div style="background-color: #dc2626; color: white; padding: 1.5rem; border-radius: 0.5rem; box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1); max-width: 28rem;">
          <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem; color: white;">Erreur</h3>
          <p style="color: white;">{{ error }}</p>
        </div>
      </div>

      <div v-else-if="success" class="flex items-center justify-center min-h-[400px]">
        <UCard>
          <div style="background-color: white; color: black; padding: 1.5rem; border-radius: 0.5rem;">
            <h3 style="font-size: 1.125rem; font-weight: 600; margin-bottom: 0.5rem; color: black;">Succès</h3>
            <p style="color: black;">Compagnie créée avec succès !</p>
          </div>
        </UCard>
      </div>

      <form v-else @submit.prevent="handleSubmit">
        <TextInput
          id="ip-name"
          v-model="formData.name"
          label="Nom"
          type="text"
          name="ip-name"
          autocomplete="name"
        />
        <SelectInput
          id="sel-pack"
          v-model="formData.packId"
          label="Pack de sponsoring"
          name="sel-pack"
        >
          <option v-for="pack in packs" :key="pack.id" :value="pack.id">
            {{ pack.name }} - {{ pack.base_price }} €
          </option>
        </SelectInput>
        <TextInput
          id="ip-email"
          v-model="formData.email"
          label="Email"
          type="email"
          name="ip-email"
          autocomplete="email"
        />
        <PhoneInput
          id="ip-phone"
          v-model="formData.phone"
          label="Tél."
          name="ip-phone"
          autocomplete="tel"
        />

        <OptionsInput
          legend="Options de sponsoring"
          :options="options"
        />

        <p class="buttons-bar">
          <input type="submit" value="Valider" :disabled="isSubmitting">
        </p>
      </form>

      <div v-if="!error && !success" class="auth-section">
        <p>Vous êtes un organisateur d'événements ?</p>
        <NuxtLink to="/login" class="login-link">Se connecter pour gérer vos organisations</NuxtLink>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { getEventsSponsoringPacks, postCompanies, postEventsPartnership, type SponsoringPack, type SponsoringOption, type CreateCompany, type RegisterPartnership } from "~/utils/api";

definePageMeta({
  layout: "minimal",
  auth: false,
});

const packs = ref<SponsoringPack[]>([]);
const options = ref<SponsoringOption[]>([]);
const isSubmitting = ref(false);
const error = ref<string | null>(null);
const success = ref(false);

const formData = ref({
  name: '',
  email: '',
  phone: '',
  packId: '',
  contactName: '',
  contactRole: '',
});

onMounted(async () => {
  const orgSlug = "test"
  const eventSlug = 'devlille';

  if (orgSlug && eventSlug) {
    try {
      const [packsResponse, optionsResponse] = await Promise.all([
        getEventsSponsoringPacks(eventSlug),
        /*getOrgsEventsOptions(orgSlug, eventSlug)*/
      ]);
      console.log(packsResponse.data)
      packs.value = packsResponse.data;
      //options.value = optionsResponse.data;
    } catch (error) {
      console.error('Failed to load sponsoring data:', error);
    }
  }
});

const handleSubmit = async () => {
  isSubmitting.value = true;
  error.value = null;
  success.value = false;

  try {
    const eventSlug = 'devlille';

    // Step 1: Create company
    const companyData: CreateCompany = {
      name: formData.value.name,
      head_office: {
        address: '',
        city: '',
        zip_code: '',
        country: '',
      },
      siret: '',
      vat: '',
      site_url: '',
    };

    const companyResponse = await postCompanies(companyData);
    console.log('Company created:', companyResponse.data);

    // Step 2: Create partnership with the company
    const partnershipData: RegisterPartnership = {
      company_id: companyResponse.data.id,
      pack_id: formData.value.packId,
      option_ids: [],
      contact_name: formData.value.contactName || formData.value.name,
      contact_role: formData.value.contactRole || '',
      language: 'fr',
      phone: formData.value.phone || null,
      emails: formData.value.email ? [formData.value.email] : [],
    };

    const partnershipResponse = await postEventsPartnership(eventSlug, partnershipData);
    console.log('Partnership created:', partnershipResponse.data);

    success.value = true;

    // Reset form after successful submission
    formData.value = {
      name: '',
      email: '',
      phone: '',
      packId: '',
      contactName: '',
      contactRole: '',
    };
  } catch (err) {
    console.error('Failed to create company or partnership:', err);
    error.value = 'Une erreur est survenue lors de la création de la demande de partenariat';
  } finally {
    isSubmitting.value = false;
  }
};

useHead({
  title: "Demande de Partenariat | DevLille",
  bodyAttrs: {
    id: "partners",
  }
});
</script>
