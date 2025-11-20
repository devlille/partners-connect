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

        <RadioInput
          v-model="formData.packId"
          legend="Pack de sponsoring"
          name="pack"
          :options="packOptions"
        />

        <OptionsInput
          v-model="formData.optionIds"
          legend="Options de sponsoring"
          :options="options"
        />

        <p class="buttons-bar">
          <input type="submit" value="Valider" :disabled="isSubmitting">
        </p>
      </form>
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
  optionIds: [] as string[],
});

const packOptions = computed(() => {
  return packs.value.map(pack => ({
    value: pack.id,
    label: pack.name,
    price: pack.base_price
  }));
});

onMounted(async () => {
  const config = useRuntimeConfig();
  const orgSlug = config.public.defaultOrgSlug;
  const eventSlug = config.public.defaultEventSlug;

  if (orgSlug && eventSlug) {
    try {
      const packsResponse = await getEventsSponsoringPacks(eventSlug);
      console.log(packsResponse.data)
      packs.value = packsResponse.data;
      //options.value = optionsResponse.data;
    } catch (error) {
      console.error('Failed to load sponsoring data:', error);
    }
  }
});

watch(() => formData.value.packId, (newPackId) => {
  // Réinitialiser les options sélectionnées quand on change de pack
  formData.value.optionIds = [];

  if (newPackId) {
    const selectedPack = packs.value.find(pack => pack.id === newPackId);
    if (selectedPack) {
      // Afficher uniquement les options optionnelles du pack sélectionné
      options.value = selectedPack.optional_options || [];
    }
  } else {
    options.value = [];
  }
});

const handleSubmit = async () => {
  isSubmitting.value = true;
  error.value = null;
  success.value = false;

  try {
    const config = useRuntimeConfig();
    const eventSlug = config.public.defaultEventSlug;

    // Step 1: Create company
    const companyData: CreateCompany = {
      name: formData.value.name,
      head_office: {
        address: '',
        city: '',
        zip_code: '',
        country: '',
      },
      siret: '00000000000000',
      vat: 'FR123456789',
      site_url: '',
    };

    const companyResponse = await postCompanies(companyData);
    console.log('Company created:', companyResponse.data);

    // Step 2: Create partnership with the company
    const partnershipData: RegisterPartnership = {
      company_id: companyResponse.data.id,
      pack_id: formData.value.packId,
      option_ids: formData.value.optionIds,
      contact_name: formData.value.contactName || formData.value.name,
      contact_role: formData.value.contactRole || '',
      language: 'fr',
      phone: formData.value.phone || null,
      emails: formData.value.email ? [formData.value.email] : [],
    };

    const partnershipResponse = await postEventsPartnership(eventSlug, partnershipData);
    console.log('Partnership created:', partnershipResponse.data);

    // Redirect to the partnership page
    const partnershipId = partnershipResponse.data.id;
    await navigateTo(`/${eventSlug}/${partnershipId}`);
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
  },
  link: [
    {
      rel: 'stylesheet',
      href: '/css/main.css'
    }
  ]
});
</script>
