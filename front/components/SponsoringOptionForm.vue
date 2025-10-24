<template>
  <form @submit.prevent="onSave">
    <!-- Onglets de langues avec bouton d'ajout -->
    <div class="mb-6">
      <div class="flex gap-2 border-b items-center">
        <button
          v-for="lang in activeLanguages"
          :key="lang.code"
          type="button"
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

    <!-- Formulaire pour la langue sélectionnée -->
    <div v-if="form.translations[currentLanguage]" class="grid grid-cols-1 gap-4 mb-6">
      <div>
        <label :for="`name-${currentLanguage}`" class="block text-sm font-medium text-gray-700 mb-1">
          Nom de l'option* ({{ getCurrentLanguageLabel() }})
        </label>
        <UInput
          :id="`name-${currentLanguage}`"
          v-model="form.translations[currentLanguage].name"
          type="text"
          required
          placeholder="Ex: Logo sur T-shirt, Stand premium..."
        />
      </div>

      <div>
        <label :for="`description-${currentLanguage}`" class="block text-sm font-medium text-gray-700 mb-1">
          Description ({{ getCurrentLanguageLabel() }})
        </label>
        <UTextarea
          :id="`description-${currentLanguage}`"
          v-model="form.translations[currentLanguage].description"
          :rows="4"
          placeholder="Description détaillée de l'option de sponsoring"
        />
      </div>
    </div>

    <!-- Prix (commun à toutes les langues) -->
    <div class="border-t pt-4 mb-6">
      <div>
        <label for="price" class="block text-sm font-medium text-gray-700 mb-1">Prix (€)</label>
        <UInput
          id="price"
          v-model.number="form.price"
          type="number"
          :min="0"
          :step="0.01"
          placeholder="0.00 (laisser vide si gratuit)"
        />
      </div>
    </div>

    <!-- Section des packs -->
    <div v-if="packs && packs.length > 0" class="border-t border-gray-200 pt-4 mb-6">
      <label class="block text-sm font-medium text-gray-700 mb-3">Associer à des packs</label>
      <div class="space-y-2">
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
    </div>

    <div class="flex justify-end gap-4 pt-4">
      <UButton type="submit" color="primary" size="lg">
        Valider
      </UButton>
    </div>
  </form>
</template>

<script lang="ts" setup>
import type { CreateSponsoringOption, SponsoringPack } from "~/utils/api";

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

// Type pour les traductions dans le formulaire
type TranslationForm = {
  name: string;
  description: string;
};

// Type pour les données du formulaire
type OptionFormData = {
  translations: Record<string, TranslationForm>;
  price: number | undefined;
  selectedPacks: string[];
};

const props = defineProps<{
  data: any;
  packs?: SponsoringPack[];
}>()

const emit = defineEmits<{
  (e: 'save', payload: any): void
}>()

// Langues actives (commence avec français uniquement)
const activeLanguages = ref<Array<{ code: string, label: string }>>([
  allAvailableLanguages[0] // Français
]);

// Langue courante sélectionnée
const currentLanguage = ref('fr');

// Initialiser le formulaire avec le français uniquement
const form = ref<OptionFormData>({
  translations: {
    fr: {
      name: props.data?.name || '',
      description: props.data?.description || ''
    }
  },
  price: props.data?.price || undefined,
  selectedPacks: props.data?.selectedPacks || []
})

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

function onSave() {
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
    alert('Veuillez remplir au moins une traduction');
    return;
  }

  const formattedData: CreateSponsoringOption = {
    translations,
    price: form.value.price || null
  };

  emit('save', {
    option: formattedData,
    selectedPacks: form.value.selectedPacks
  })
}
</script>
