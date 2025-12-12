<template>
  <div class="space-y-6">
    <!-- Logo Upload -->
    <div v-if="!readonly">
      <LogoUpload
        :company-id="company.id"
        :company-name="company.name"
        :current-logo-media="company.medias || null"
        :disabled="loading"
        @uploaded="handleLogoUploaded"
        @deleted="handleLogoDeleted"
        @error="handleLogoError"
      />
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-6">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label for="name" class="block text-sm font-medium text-gray-700 mb-2">
            Nom de l'entreprise
          </label>
          <UInput
            id="name"
            v-model="form.name"
            placeholder="Nom de l'entreprise"
            :disabled="readonly || loading"
            class="w-full"
          />
        </div>

        <SiretInput
          v-model="form.siret"
          :disabled="readonly || loading"
          :required="true"
          @validation="handleSiretValidation"
        />

        <VatInput v-model="form.vat" :disabled="readonly || loading" />

        <UrlInput
          v-model="form.site_url"
          label="Site web"
          :required="true"
          :disabled="readonly || loading"
        />

        <div class="md:col-span-2">
          <label for="description" class="block text-sm font-medium text-gray-700 mb-2">
            Description
          </label>
          <textarea
            id="description"
            v-model="form.description"
            rows="4"
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="Description de l'entreprise"
            :disabled="readonly || loading"
          />
          <p class="text-xs text-gray-500 mt-1">
            Cette information sera publiée sur la page dédiée au partenaire sur le site de la
            conférence
          </p>
        </div>
      </div>

      <div class="border-t pt-4">
        <h3 class="text-sm font-semibold text-gray-900 mb-4">Adresse du siège social</h3>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="md:col-span-2">
            <label for="address" class="block text-sm font-medium text-gray-700 mb-2">
              Adresse<span class="text-red-500 ml-1">*</span>
            </label>
            <UInput
              id="address"
              v-model="form.head_office!.address"
              placeholder="Rue, numéro..."
              :disabled="readonly || loading"
              class="w-full"
              required
            />
          </div>

          <ZipCodeInput
            v-model="form.head_office!.zip_code"
            :disabled="readonly || loading"
            required
          />

          <div>
            <label for="city" class="block text-sm font-medium text-gray-700 mb-2">
              Ville<span class="text-red-500 ml-1">*</span>
            </label>
            <UInput
              id="city"
              v-model="form.head_office!.city"
              placeholder="Ville"
              :disabled="readonly || loading"
              class="w-full"
              required
            />
          </div>

          <div class="md:col-span-2">
            <label for="country" class="block text-sm font-medium text-gray-700 mb-2">
              Pays<span class="text-red-500 ml-1">*</span>
            </label>
            <UInput
              id="country"
              v-model="form.head_office!.country"
              placeholder="France"
              :disabled="readonly || loading"
              class="w-full"
              required
            />
          </div>
        </div>
      </div>

      <div class="border-t pt-4">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold text-gray-900">Réseaux sociaux</h3>
          <UButton
            v-if="!readonly"
            type="button"
            color="primary"
            variant="ghost"
            size="sm"
            @click="addSocial"
          >
            + Ajouter un réseau
          </UButton>
        </div>

        <div v-if="socials.length === 0" class="text-sm text-gray-500 italic">
          Aucun réseau social défini
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="(social, index) in socials"
            :key="index"
            class="grid grid-cols-1 md:grid-cols-[200px_1fr_auto] gap-3 items-start"
          >
            <div>
              <USelectMenu
                :model-value="getSelectedSocialType(social.type)"
                :items="socialTypeOptions"
                :disabled="readonly || loading"
                class="w-full"
                @update:model-value="(selected) => updateSocialType(index, selected)"
              />
            </div>
            <div>
              <UrlInput
                v-model="social.url"
                placeholder="https://..."
                :disabled="readonly || loading"
                :validate="false"
              />
            </div>
            <div>
              <UButton
                v-if="!readonly"
                type="button"
                color="error"
                variant="ghost"
                size="sm"
                @click="removeSocial(index)"
              >
                -
              </UButton>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!readonly" class="flex gap-3 pt-4">
        <UButton type="submit" color="primary" :loading="loading" :disabled="!isFormValid">
          Mettre à jour les informations
        </UButton>
      </div>
      <div v-if="!readonly && !isFormValid" class="text-sm text-red-600 pt-2">
        <span v-if="form.siret && !isSiretValid"
          >Le SIRET doit contenir exactement 14 chiffres</span
        >
        <span v-else
          >Veuillez remplir tous les champs obligatoires (SIRET, site web et adresse complète)</span
        >
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import type { UpdateCompanySchema, CompanySchema, SocialSchema, MediaSchema } from '~/utils/api';
import { SocialSchemaType as SocialTypes } from '~/utils/api';
import LogoUpload from '~/components/LogoUpload.vue';

interface Props {
  company: CompanySchema;
  readonly?: boolean;
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  readonly: false,
  loading: false
});

const emit = defineEmits<{
  save: [data: UpdateCompanySchema];
}>();

// Options pour le select des réseaux sociaux
const socialTypeOptions = [
  { value: SocialTypes.LINKEDIN, label: 'LinkedIn' },
  { value: SocialTypes.X, label: 'X (Twitter)' },
  { value: SocialTypes.BLUESKY, label: 'Bluesky' },
  { value: SocialTypes.MASTODON, label: 'Mastodon' },
  { value: SocialTypes.INSTAGRAM, label: 'Instagram' },
  { value: SocialTypes.FACEBOOK, label: 'Facebook' },
  { value: SocialTypes.YOUTUBE, label: 'YouTube' }
];

// Liste réactive des réseaux sociaux
const socials = ref<SocialSchema[]>([]);

// Initialiser les réseaux sociaux depuis les props
function initializeSocials() {
  // Vérifier si l'API retourne les socials (le type ne l'inclut pas encore mais l'API pourrait le faire)
  const companySocials = (props.company as any).socials;
  if (companySocials && Array.isArray(companySocials)) {
    socials.value = [...companySocials];
  } else {
    socials.value = [];
  }
}

const form = ref<UpdateCompanySchema>({
  name: props.company.name,
  siret: props.company.siret,
  vat: props.company.vat,
  site_url: props.company.site_url,
  description: props.company.description || '',
  head_office: props.company.head_office ? {
    address: props.company.head_office.address,
    city: props.company.head_office.city,
    zip_code: props.company.head_office.zip_code,
    country: props.company.head_office.country
  } : {
    address: '',
    city: '',
    zip_code: '',
    country: ''
  }
});

// Ajouter un réseau social
function addSocial() {
  socials.value.push({
    type: SocialTypes.LINKEDIN,
    url: ''
  });
}

// Supprimer un réseau social
function removeSocial(index: number) {
  socials.value.splice(index, 1);
}

// Obtenir l'option sélectionnée pour le type de réseau social
function getSelectedSocialType(type: string) {
  return socialTypeOptions.find(option => option.value === type);
}

// Mettre à jour le type de réseau social
function updateSocialType(index: number, selected: any) {
  if (selected && selected.value) {
    socials.value[index].type = selected.value;
  }
}

// État de validation du SIRET (géré par le composant SiretInput)
const siretIsValid = ref(false);

// Handler pour la validation du SIRET
function handleSiretValidation(isValid: boolean) {
  siretIsValid.value = isValid;
}

// Vérifie si le SIRET est valide (14 chiffres)
const isSiretValid = computed(() => {
  if (!form.value.siret) return false;
  return siretIsValid.value;
});

// Vérifie si le formulaire est valide (tous les champs requis sont remplis)
const isFormValid = computed(() => {
  return !!(
    form.value.siret &&
    isSiretValid.value &&
    form.value.site_url &&
    form.value.head_office?.address &&
    form.value.head_office?.city &&
    form.value.head_office?.zip_code &&
    form.value.head_office?.country
  );
});

function handleSubmit() {
  // Vérifier la validité du formulaire avant de soumettre
  if (!isFormValid.value) {
    return;
  }

  // Filtrer les réseaux sociaux valides (avec une URL)
  const validSocials = socials.value.filter(social => social.url.trim() !== '');

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
    },
    socials: validSocials.length > 0 ? validSocials : null
  };

  emit('save', updateData);
}

// Mettre à jour le formulaire si les props changent
watch(() => props.company, (newCompany) => {
  form.value = {
    name: newCompany.name,
    siret: newCompany.siret,
    vat: newCompany.vat,
    site_url: newCompany.site_url,
    description: newCompany.description || '',
    head_office: newCompany.head_office ? {
      address: newCompany.head_office.address,
      city: newCompany.head_office.city,
      zip_code: newCompany.head_office.zip_code,
      country: newCompany.head_office.country
    } : {
      address: '',
      city: '',
      zip_code: '',
      country: ''
    }
  };
  initializeSocials();
}, { deep: true });

// Initialiser au montage du composant
onMounted(() => {
  initializeSocials();
});

/**
 * Handle logo upload success
 */
function handleLogoUploaded(media: MediaSchema) {
  console.log('Logo uploaded successfully:', media);
  // Reload company data could be done via emit to parent
  emit('save' as any, form.value); // Trigger parent reload
}

/**
 * Handle logo deletion
 */
function handleLogoDeleted() {
  console.log('Logo deleted successfully');
  emit('save' as any, form.value); // Trigger parent reload
}

/**
 * Handle logo upload error
 */
function handleLogoError(message: string) {
  console.error('Logo upload error:', message);
}
</script>
