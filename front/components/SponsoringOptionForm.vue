<template>
  <form @submit.prevent="onSave" novalidate>
    <!-- Sélecteur de type d'option -->
    <div class="mb-6">
      <label for="option-type" class="block text-sm font-medium text-gray-700 mb-2">
        Type d'option*
      </label>
      <select
        id="option-type"
        v-model="form.type"
        class="w-full px-3 py-2 border border-gray-300 rounded-md bg-white focus:outline-none focus:ring-2 focus:ring-primary-500"
      >
        <option v-for="type in optionTypes" :key="type.value" :value="type.value">
          {{ type.label }}
        </option>
      </select>
      <p class="mt-1 text-sm text-gray-500">
        Sélectionnez le type d'option qui correspond à votre besoin
      </p>
    </div>

    <!-- Onglets de langues avec bouton d'ajout -->
    <div class="mb-6">
      <div class="flex gap-2 border-b items-center" role="tablist" aria-label="Langues disponibles">
        <button
          v-for="lang in activeLanguages"
          :key="lang.code"
          type="button"
          role="tab"
          :aria-selected="currentLanguage === lang.code"
          :aria-controls="`panel-${lang.code}`"
          class="px-4 py-3 text-base font-semibold transition-colors relative inline-flex items-center gap-2"
          :class="currentLanguage === lang.code
            ? 'text-primary-600 border-b-2 border-primary-600'
            : 'text-gray-600 hover:text-gray-900'"
          @click="currentLanguage = lang.code"
        >
          <span>{{ lang.label }}</span>
          <button
            v-if="lang.code !== 'fr'"
            type="button"
            class="text-red-500 hover:text-red-700 font-bold text-lg"
            @click.stop="removeLanguage(lang.code)"
          >
            ×
          </button>
        </button>

        <!-- Bouton d'ajout de langue -->
        <div v-if="getLanguagesNotAdded().length > 0" class="ml-auto">
          <select
            class="px-3 py-2 text-sm border border-gray-300 rounded-md bg-white"
            @change="(e) => { addLanguage((e.target as HTMLSelectElement).value); (e.target as HTMLSelectElement).value = ''; }"
          >
            <option value="">+ Ajouter une langue</option>
            <option v-for="lang in getLanguagesNotAdded()" :key="lang.code" :value="lang.code">
              {{ lang.label }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- Message d'erreur général pour les traductions -->
    <p
      v-if="validationErrors['translations']"
      class="mb-4 text-sm text-red-600"
      role="alert"
      aria-live="polite"
    >
      {{ validationErrors['translations'] }}
    </p>

    <!-- Formulaire pour la langue sélectionnée -->
    <div
      v-if="currentTranslation"
      :id="`panel-${currentLanguage}`"
      role="tabpanel"
      :aria-labelledby="`tab-${currentLanguage}`"
      class="grid grid-cols-1 gap-4 mb-6"
    >
      <div>
        <label
          :for="`name-${currentLanguage}`"
          class="block text-sm font-medium text-gray-700 mb-1"
        >
          Nom de l'option* ({{ getCurrentLanguageLabel() }})
        </label>
        <UInput
          :id="`name-${currentLanguage}`"
          v-model="currentTranslation.name"
          type="text"
          required
          placeholder="Ex: Logo sur T-shirt, Stand premium..."
          class="w-full"
          :class="{ 'border-red-500': validationErrors[`translations.${currentLanguage}.name`] }"
          :aria-invalid="!!validationErrors[`translations.${currentLanguage}.name`]"
          :aria-describedby="validationErrors[`translations.${currentLanguage}.name`] ? `name-${currentLanguage}-error` : undefined"
        />
        <p
          v-if="validationErrors[`translations.${currentLanguage}.name`]"
          :id="`name-${currentLanguage}-error`"
          class="mt-1 text-sm text-red-600"
          role="alert"
        >
          {{ validationErrors[`translations.${currentLanguage}.name`] }}
        </p>
      </div>

      <div>
        <label
          :for="`description-${currentLanguage}`"
          class="block text-sm font-medium text-gray-700 mb-1"
        >
          Description ({{ getCurrentLanguageLabel() }})
        </label>
        <UTextarea
          :id="`description-${currentLanguage}`"
          v-model="currentTranslation.description"
          :rows="4"
          placeholder="Description détaillée de l'option de sponsoring"
          class="w-full"
        />
      </div>
    </div>

    <!-- Prix (commun à toutes les langues, sauf pour typed_selectable) -->
    <div v-if="form.type !== 'typed_selectable'" class="border-t pt-4 mb-6">
      <div>
        <label for="price" class="block text-sm font-medium text-gray-700 mb-1">Prix (€)</label>
        <UInput
          id="price"
          v-model.number="form.price"
          type="number"
          :min="0"
          :step="0.01"
          placeholder="0.00 (laisser vide si gratuit)"
          class="w-full"
          :class="{ 'border-red-500': validationErrors.price }"
          :aria-invalid="!!validationErrors.price"
          :aria-describedby="validationErrors.price ? 'price-error' : undefined"
        />
        <p
          v-if="validationErrors.price"
          id="price-error"
          class="mt-1 text-sm text-red-600"
          role="alert"
        >
          {{ validationErrors.price }}
        </p>
      </div>
    </div>

    <!-- Champs spécifiques pour typed_number -->
    <div v-if="form.type === 'typed_number'" class="border-t pt-4 mb-6">
      <div>
        <label for="fixed-quantity" class="block text-sm font-medium text-gray-700 mb-1">
          Quantité fixe*
        </label>
        <UInput
          id="fixed-quantity"
          v-model.number="form.fixedQuantity"
          type="number"
          :min="1"
          :step="1"
          placeholder="Ex: 10"
          class="w-full"
          required
        />
        <p class="mt-1 text-sm text-gray-500">
          Cette quantité ne pourra pas être modifiée par les sponsors
        </p>
      </div>
    </div>

    <!-- Champs spécifiques pour typed_selectable -->
    <div v-if="form.type === 'typed_selectable'" class="border-t pt-4 mb-6">
      <div class="mb-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Valeurs sélectionnables*
        </label>
        <p class="text-sm text-gray-500 mb-3">
          Ajoutez les différentes options disponibles avec leurs prix individuels
        </p>

        <!-- Liste des valeurs -->
        <div
          v-for="(selectableValue, index) in form.selectableValues"
          :key="index"
          class="flex gap-2 mb-2"
        >
          <UInput
            v-model="selectableValue.value"
            type="text"
            placeholder="Ex: Stand 3x3m"
            class="flex-1"
            required
          />
          <UInput
            v-model.number="selectableValue.price"
            type="number"
            :min="0"
            :step="0.01"
            placeholder="Prix (€)"
            class="w-32"
            required
          />
          <button
            type="button"
            class="px-3 py-2 text-red-600 hover:text-red-800 font-bold"
            @click="removeSelectableValue(index)"
          >
            ×
          </button>
        </div>

        <!-- Bouton d'ajout -->
        <button
          type="button"
          class="mt-2 px-4 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
          @click="addSelectableValue"
        >
          + Ajouter une valeur
        </button>
      </div>
    </div>

    <!-- Section des packs -->
    <div v-if="packs && packs.length > 0" class="border-t border-gray-200 pt-4 mb-6">
      <fieldset>
        <legend class="block text-sm font-medium text-gray-700 mb-3">Associer à des packs</legend>
        <div class="space-y-2" role="group" aria-label="Packs disponibles">
          <div v-for="pack in packs" :key="pack.id" class="flex items-center space-x-3">
            <input
              :id="`pack-${pack.id}`"
              v-model="form.selectedPacks"
              type="checkbox"
              :value="pack.id"
              class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
            />
            <label :for="`pack-${pack.id}`" class="text-sm text-gray-700 cursor-pointer">
              {{ pack.name }} ({{ pack.base_price }}€)
            </label>
          </div>
        </div>
      </fieldset>
    </div>

    <div class="flex justify-end gap-4 pt-4">
      <UButton type="submit" color="primary" size="lg"> Valider </UButton>
    </div>
  </form>
</template>

<script lang="ts" setup>
import type { CreateSponsoringOptionSchema, SponsoringPack, CreateSelectableValue } from "~/utils/api";

// Toutes les langues disponibles
const allAvailableLanguages = [
  { code: 'fr', label: 'Français' },
  { code: 'en', label: 'English' },
  { code: 'es', label: 'Español' },
  { code: 'de', label: 'Deutsch' },
  { code: 'it', label: 'Italiano' },
  { code: 'pt', label: 'Português' },
  { code: 'nl', label: 'Nederlands' },
  { code: 'pl', label: 'Polski' },
  { code: 'ru', label: 'Русский' },
  { code: 'zh', label: '中文' },
  { code: 'ja', label: '日本語' },
  { code: 'ar', label: 'العربية' }
];

// Types d'options disponibles
const optionTypes = [
  { value: 'text', label: 'Texte libre' },
  { value: 'typed_quantitative', label: 'Quantité (ex: offres d\'emploi)' },
  { value: 'typed_number', label: 'Nombre fixe (ex: billets)' },
  { value: 'typed_selectable', label: 'Sélection (ex: stands)' }
];

// Type pour les traductions dans le formulaire
type TranslationForm = {
  name: string;
  description: string;
};

// Type pour les valeurs sélectionnables
type SelectableValueForm = {
  value: string;
  price: number;
};

// Type pour les données du formulaire
type OptionFormData = {
  type: 'text' | 'typed_quantitative' | 'typed_number' | 'typed_selectable';
  translations: Record<string, TranslationForm>;
  price: number | undefined;
  selectedPacks: string[];
  // Champs spécifiques pour typed_quantitative
  typeDescriptorQuantitative?: string;
  // Champs spécifiques pour typed_number
  typeDescriptorNumber?: string;
  fixedQuantity?: number;
  // Champs spécifiques pour typed_selectable
  typeDescriptorSelectable?: string;
  selectableValues: SelectableValueForm[];
};

const props = defineProps<{
  data: any;
  packs?: SponsoringPack[];
}>()

const emit = defineEmits<{
  (e: 'save', payload: any): void
}>()

// Fonction pour initialiser les langues actives
function initializeActiveLanguages(): Array<{ code: string, label: string }> {
  if (props.data?.translations && typeof props.data.translations === 'object') {
    const langs = Object.keys(props.data.translations);
    return langs.map(code => {
      const lang = allAvailableLanguages.find(l => l.code === code);
      return lang || { code, label: code.toUpperCase() };
    });
  }
  // Par défaut, français uniquement
  return [allAvailableLanguages[0]!];
}

// Langues actives
const activeLanguages = ref<Array<{ code: string, label: string }>>(initializeActiveLanguages());

// Langue courante sélectionnée
const currentLanguage = ref(activeLanguages.value[0]?.code || 'fr');

// Computed pour la traduction courante
const currentTranslation = computed(() => form.value.translations[currentLanguage.value]);

// Fonction pour initialiser le formulaire en fonction des données
function initializeForm(): OptionFormData {
  // Initialiser les traductions
  let initialTranslations: Record<string, TranslationForm> = {
    fr: {
      name: '',
      description: ''
    }
  };

  // Si on a des données avec translations (format objet dictionnaire)
  if (props.data?.translations && typeof props.data.translations === 'object') {
    initialTranslations = {};
    Object.entries(props.data.translations).forEach(([lang, translation]: [string, any]) => {
      initialTranslations[lang] = {
        name: translation.name || '',
        description: translation.description || ''
      };
    });
  }
  // Sinon, fallback sur name/description directs
  else if (props.data?.name) {
    initialTranslations.fr = {
      name: props.data.name || '',
      description: props.data.description || ''
    };
  }

  const baseForm: OptionFormData = {
    type: props.data?.type || 'text',
    translations: initialTranslations,
    price: props.data?.price || undefined,
    selectedPacks: props.data?.selectedPacks || [],
    typeDescriptorQuantitative: 'job_offer',
    typeDescriptorNumber: 'nb_ticket',
    fixedQuantity: 1,
    typeDescriptorSelectable: 'booth',
    selectableValues: []
  };

  // Si on a des données existantes, initialiser selon le type
  if (props.data) {
    switch (props.data.type) {
      case 'typed_number':
        baseForm.fixedQuantity = (props.data as any).fixed_quantity || 1;
        baseForm.typeDescriptorNumber = (props.data as any).type_descriptor || 'nb_ticket';
        break;

      case 'typed_quantitative':
        baseForm.typeDescriptorQuantitative = (props.data as any).type_descriptor || 'job_offer';
        break;

      case 'typed_selectable':
        baseForm.typeDescriptorSelectable = (props.data as any).type_descriptor || 'booth';
        // Convertir les valeurs sélectionnables
        if ((props.data as any).selectable_values && Array.isArray((props.data as any).selectable_values)) {
          baseForm.selectableValues = (props.data as any).selectable_values.map((v: any) => {
            // Si v est un objet avec price (format CreateSelectableValue)
            if (typeof v === 'object' && 'price' in v) {
              return {
                value: v.value,
                price: v.price / 100 // Convertir centimes -> euros
              };
            }
            // Si v est juste une string (format SponsoringOptionSchema)
            return {
              value: typeof v === 'string' ? v : v.value || '',
              price: 0 // Prix par défaut à 0, l'utilisateur devra le saisir
            };
          });
        }
        break;
    }
  }

  return baseForm;
}

// Initialiser le formulaire
const form = ref<OptionFormData>(initializeForm())

const validationErrors = ref<Record<string, string>>({});

function getCurrentLanguageLabel() {
  return activeLanguages.value.find(lang => lang.code === currentLanguage.value)?.label || '';
}

function getLanguagesNotAdded() {
  return allAvailableLanguages.filter(
    lang => !activeLanguages.value.some(active => active.code === lang.code)
  );
}

function addLanguage(langCode: string) {
  if (!langCode) return;

  const lang = allAvailableLanguages.find(l => l.code === langCode);
  if (lang && !activeLanguages.value.some(l => l.code === langCode)) {
    activeLanguages.value.push(lang);
    form.value.translations[langCode] = {
      name: '',
      description: ''
    };
    currentLanguage.value = langCode;
  }
}

function removeLanguage(langCode: string) {
  if (langCode === 'fr') {
    return; // Ne pas permettre de supprimer le français
  }
  activeLanguages.value = activeLanguages.value.filter(l => l.code !== langCode);
  delete form.value.translations[langCode];
  if (currentLanguage.value === langCode) {
    currentLanguage.value = 'fr';
  }
}

function addSelectableValue() {
  form.value.selectableValues.push({
    value: '',
    price: 0
  });
}

function removeSelectableValue(index: number) {
  form.value.selectableValues.splice(index, 1);
}

function onSave() {
  validationErrors.value = {};

  // Construire le tableau de traductions uniquement pour les langues actives et remplies
  const translations = activeLanguages.value
    .filter(lang => {
      const translation = form.value.translations[lang.code];
      return translation && translation.name.trim() !== '';
    })
    .map(lang => {
      const translation = form.value.translations[lang.code]!;
      return {
        language: lang.code,
        name: translation.name,
        description: translation.description || null
      };
    });

  // Vérifier qu'au moins une traduction existe
  if (translations.length === 0) {
    validationErrors.value['translations'] = 'Au moins une traduction est requise';
    return;
  }

  // Construire les données selon le type
  let formattedData: CreateSponsoringOptionSchema;

  switch (form.value.type) {
    case 'text':
      formattedData = {
        type: 'text',
        translations,
        price: form.value.price ?? null
      };
      break;

    case 'typed_quantitative':
      formattedData = {
        type: 'typed_quantitative',
        translations,
        price: form.value.price ?? null,
        type_descriptor: form.value.typeDescriptorQuantitative as 'job_offer'
      };
      break;

    case 'typed_number':
      if (!form.value.fixedQuantity || form.value.fixedQuantity < 1) {
        validationErrors.value['fixedQuantity'] = 'La quantité fixe doit être au moins 1';
        return;
      }
      formattedData = {
        type: 'typed_number',
        translations,
        price: form.value.price ?? null,
        type_descriptor: form.value.typeDescriptorNumber as 'nb_ticket',
        fixed_quantity: form.value.fixedQuantity
      };
      break;

    case 'typed_selectable':
      if (form.value.selectableValues.length === 0) {
        validationErrors.value['selectableValues'] = 'Au moins une valeur sélectionnable est requise';
        return;
      }

      // Valider les valeurs sélectionnables
      const hasEmptyValues = form.value.selectableValues.some(v => !v.value.trim());
      if (hasEmptyValues) {
        validationErrors.value['selectableValues'] = 'Toutes les valeurs doivent avoir un nom';
        return;
      }

      // Convertir les prix en centimes
      const selectableValues: CreateSelectableValue[] = form.value.selectableValues.map(v => ({
        value: v.value,
        price: Math.round(v.price * 100) // Convertir en centimes
      }));

      formattedData = {
        type: 'typed_selectable',
        translations,
        price: null, // Pour typed_selectable, le prix est dans les valeurs
        type_descriptor: form.value.typeDescriptorSelectable as 'booth',
        selectable_values: selectableValues
      };
      break;

    default:
      validationErrors.value['type'] = 'Type d\'option invalide';
      return;
  }

  emit('save', {
    option: formattedData,
    selectedPacks: form.value.selectedPacks
  })
}
</script>
