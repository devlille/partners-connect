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

      <form v-else @submit.prevent="handleSubmit" novalidate>
        <TextInput
          id="ip-name"
          v-model="formData.name"
          label="Nom de la société"
          type="text"
          name="ip-name"
          autocomplete="name"
          :aria-describedby="errors.name ? 'ip-name-error' : undefined"
          :class="{ 'error': errors.name }"
        />
        <div v-if="errors.name" id="ip-name-error" class="error-message" role="alert" style="color: white; margin-bottom: 1rem;">
          {{ errors.name }}
        </div>

        <TextInput
          id="ip-email"
          v-model="formData.email"
          label="Email"
          type="email"
          name="ip-email"
          autocomplete="email"
          :aria-describedby="errors.email ? 'ip-email-error' : undefined"
          :class="{ 'error': errors.email }"
        />
        <div v-if="errors.email" id="ip-email-error" class="error-message" role="alert" style="color: white; margin-bottom: 1rem;">
          {{ errors.email }}
        </div>

        <PhoneInput
          id="ip-phone"
          v-model="formData.phone"
          label="Tél."
          name="ip-phone"
          autocomplete="tel"
          :aria-describedby="errors.phone ? 'ip-phone-error' : undefined"
          :class="{ 'error': errors.phone }"
        />
        <div v-if="errors.phone" id="ip-phone-error" class="error-message" role="alert" style="color: white; margin-bottom: 1rem;">
          {{ errors.phone }}
        </div>

        <div :class="{ 'error': errors.packId }">
          <RadioInput
            v-model="formData.packId"
            legend="Pack de sponsoring"
            name="pack"
            :options="packOptions"
            :aria-describedby="errors.packId ? 'pack-error' : undefined"
          />
        </div>
        <div v-if="errors.packId" id="pack-error" class="error-message" role="alert" style="color: white; margin-bottom: 1rem;">
          {{ errors.packId }}
        </div>

        <SponsoringOptionsInput
          v-model="formData.optionSelections"
          legend="Options de sponsoring supplémentaires"
          :options="options"
        />

        <!-- Total Summary -->
        <fieldset v-if="formData.packId" style="border: 2px solid #fbbf24; padding: 1.5rem; margin: 1.5rem 0; box-sizing: border-box; background-color: rgba(251, 191, 36, 0.1);">
          <legend style="color: #fbbf24; font-weight: 700; font-size: 1.1rem; padding: 0 0.5rem;">Récapitulatif</legend>

          <div style="color: white;">
            <!-- Pack Price -->
            <div style="display: flex; justify-content: space-between; margin-bottom: 0.75rem; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255, 255, 255, 0.2);">
              <span>Pack {{ selectedPackName }}</span>
              <span style="font-weight: 600;">{{ selectedPackPrice }} €</span>
            </div>

            <!-- Options Prices -->
            <div v-if="selectedOptionsTotal > 0" style="margin-bottom: 0.75rem; padding-bottom: 0.75rem; border-bottom: 1px solid rgba(255, 255, 255, 0.2);">
              <div style="display: flex; justify-content: space-between; margin-bottom: 0.5rem;">
                <span>Options ({{ formData.optionSelections.length }})</span>
                <span style="font-weight: 600;">{{ selectedOptionsTotal }} €</span>
              </div>
              <div v-for="(selection, index) in formData.optionSelections" :key="index" style="font-size: 0.875rem; color: #cbd5e1; margin-left: 1rem; margin-top: 0.25rem;">
                <div style="display: flex; justify-content: space-between;">
                  <span>• {{ getOptionName(selection.option_id) }}</span>
                  <span>{{ getOptionTotal(selection) }} €</span>
                </div>
              </div>
            </div>

            <!-- Total -->
            <div style="display: flex; justify-content: space-between; font-size: 1.25rem; font-weight: 700; color: #fbbf24; margin-top: 1rem;">
              <span>Total</span>
              <span>{{ totalPrice }} €</span>
            </div>
          </div>
        </fieldset>

        <p class="buttons-bar">
          <input type="submit" value="Valider" :disabled="isSubmitting">
        </p>
      </form>
    </main>
  </div>
</template>

<script setup lang="ts">
import { z } from 'zod';
import { getEventsSponsoringPacks, postCompanies, postEventsPartnership, type SponsoringPack, type SponsoringOptionSchema, type CreateCompanySchema, type RegisterPartnershipSchema, type PartnershipOptionSelection } from "~/utils/api";

definePageMeta({
  layout: "minimal",
  auth: false,
});

// Schéma de validation Zod
const formSchema = z.object({
  name: z.string().min(1, 'Le nom est obligatoire'),
  email: z.string().min(1, 'L\'email est obligatoire').email({ message: 'L\'email doit être valide' }),
  phone: z.string().min(1, 'Le téléphone est obligatoire'),
  packId: z.string().min(1, 'Vous devez choisir au moins un pack'),
  contactName: z.string().optional(),
  contactRole: z.string().optional(),
  optionIds: z.array(z.string()).optional(),
});

const packs = ref<SponsoringPack[]>([]);
const options = ref<SponsoringOptionSchema[]>([]);
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
  optionSelections: [] as PartnershipOptionSelection[],
});

const errors = ref({
  name: '',
  email: '',
  phone: '',
  packId: '',
});

const packOptions = computed(() => {
  return packs.value.map(pack => ({
    value: pack.id,
    label: pack.name,
    price: pack.base_price
  }));
});

// Get selected pack
const selectedPack = computed(() => {
  if (!formData.value.packId) return null;
  return packs.value.find(pack => pack.id === formData.value.packId);
});

// Get selected pack name
const selectedPackName = computed(() => {
  return selectedPack.value?.name || '';
});

// Get selected pack price
const selectedPackPrice = computed(() => {
  return selectedPack.value?.base_price || 0;
});

// Get option by ID
function getOptionById(optionId: string): SponsoringOptionSchema | undefined {
  return options.value.find(opt => opt.id === optionId);
}

// Get option name by ID
function getOptionName(optionId: string): string {
  const option = getOptionById(optionId);
  return option?.name || 'Option inconnue';
}

// Calculate total for a single option selection
function getOptionTotal(selection: PartnershipOptionSelection): number {
  const option = getOptionById(selection.option_id);
  if (!option) return 0;

  // For quantitative options, multiply by quantity
  if (selection.selected_quantity && option.price) {
    return option.price * selection.selected_quantity;
  }

  // For selectable options, check if the selected value has a price
  if (selection.selected_value_id && option.type === 'typed_selectable') {
    // Try to find the price in selectable_values if they are objects
    const selectableValue = (option as any).selectable_values?.find((v: any) => {
      if (typeof v === 'object') {
        return (v.id || v.value) === selection.selected_value_id;
      }
      return v === selection.selected_value_id;
    });

    // If the value has its own price, use it
    if (selectableValue && typeof selectableValue === 'object' && selectableValue.price) {
      return selectableValue.price;
    }

    // Otherwise use the option's price
    return option.price || 0;
  }

  // For other options (text, typed_number), just return the price
  return option.price || 0;
}

// Calculate total of all selected options
const selectedOptionsTotal = computed(() => {
  return formData.value.optionSelections.reduce((total, selection) => {
    return total + getOptionTotal(selection);
  }, 0);
});

// Calculate grand total (pack + options)
const totalPrice = computed(() => {
  return selectedPackPrice.value + selectedOptionsTotal.value;
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
  formData.value.optionSelections = [];

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
  // Réinitialiser les erreurs
  errors.value = {
    name: '',
    email: '',
    phone: '',
    packId: '',
  };

  // Validation avec Zod
  const result = formSchema.safeParse(formData.value);

  if (!result.success) {
    // Extraire les erreurs de Zod et les mapper aux champs
    result.error.issues.forEach((issue) => {
      const field = issue.path[0] as keyof typeof errors.value;
      if (field in errors.value) {
        errors.value[field] = issue.message;
      }
    });
    return;
  }

  isSubmitting.value = true;
  error.value = null;
  success.value = false;

  try {
    const config = useRuntimeConfig();
    const eventSlug = config.public.defaultEventSlug;

    // Step 1: Create company
    const companyData: CreateCompany = {
      name: formData.value.name
    };

    const companyResponse = await postCompanies(companyData);
    console.log('Company created:', companyResponse.data);

    // Step 2: Create partnership with the company
    const partnershipData: RegisterPartnershipSchema = {
      company_id: companyResponse.data.id,
      pack_id: formData.value.packId,
      option_selections: formData.value.optionSelections.length > 0 ? formData.value.optionSelections : undefined,
      contact_name: formData.value.contactName || formData.value.name,
      contact_role: formData.value.contactRole || '',
      language: 'fr',
      phone: formData.value.phone || null,
      emails: formData.value.email ? [formData.value.email] : [],
    };

    const partnershipResponse = await postEventsPartnership(eventSlug, partnershipData);
    console.log('Partnership created:', partnershipResponse.data);

    const partnershipId = partnershipResponse.data.id;
    await navigateTo(`/${eventSlug}/${partnershipId}`);
  } catch (err: any) {
    console.error('Failed to create company or partnership:', err);

    // Gérer les erreurs spécifiques du backend
    if (err.response?.data?.message === 'Partnership submissions have not started yet') {
      error.value = 'Les inscriptions pour ce partenariat ne sont pas encore ouvertes. Veuillez réessayer ultérieurement.';
    } else if (err.response?.data?.message === 'Partnership submissions have ended') {
      error.value = 'Les inscriptions pour ce partenariat sont maintenant fermées.';
    } else if (err.response?.data?.message) {
      error.value = err.response.data.message;
    } else {
      error.value = 'Une erreur est survenue lors de la création de la demande de partenariat';
    }
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
