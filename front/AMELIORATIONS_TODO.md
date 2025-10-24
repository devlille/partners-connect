# Guide d'implémentation des améliorations restantes

Ce document liste toutes les améliorations à implémenter avec le code nécessaire.

## ✅ Déjà implémenté

1. **useOptionTranslation composable** - Évite la duplication de code pour récupérer les traductions
2. **useErrorHandler composable** - Gestion centralisée des erreurs avec i18n

---

## 🔄 À implémenter

### 3. Validation avec Zod

#### Installation
```bash
pnpm add zod
```

#### Créer les schémas de validation

**`utils/validation/schemas.ts`**
```typescript
import { z } from 'zod';

// Schéma pour un pack
export const packSchema = z.object({
  name: z.string().min(3, 'Le nom doit contenir au moins 3 caractères'),
  price: z.number().min(0, 'Le prix ne peut pas être négatif'),
  nb_tickets: z.number().int().min(0, 'Le nombre de billets doit être positif'),
  max_quantity: z.number().int().min(1).optional().or(z.literal(undefined)),
  with_booth: z.boolean()
});

// Schéma pour une option
export const optionSchema = z.object({
  translations: z.array(z.object({
    language: z.string(),
    name: z.string().min(1, 'Le nom est requis'),
    description: z.string().nullable()
  })).min(1, 'Au moins une traduction est requise'),
  price: z.number().min(0).nullable()
});

// Schéma pour un sponsor
export const sponsorSchema = z.object({
  company_name: z.string().min(2, 'Le nom de l\'entreprise est requis'),
  contact_name: z.string().min(2, 'Le nom du contact est requis'),
  contact_role: z.string().optional(),
  email: z.string().email('Email invalide'),
  phone: z.string().optional(),
  pack_id: z.string().min(1, 'Veuillez sélectionner un pack'),
  option_ids: z.array(z.string())
});
```

#### Utiliser dans les composants

**Exemple dans `SponsoringPackForm.vue`**
```typescript
import { packSchema } from '~/utils/validation/schemas';

const validationErrors = ref<Record<string, string>>({});

function validateForm(): boolean {
  try {
    const { requiredOptions, optionalOptions, ...packData } = form.value;
    packSchema.parse(packData);
    validationErrors.value = {};
    return true;
  } catch (error) {
    if (error instanceof z.ZodError) {
      validationErrors.value = {};
      error.errors.forEach(err => {
        const field = err.path[0] as string;
        validationErrors.value[field] = err.message;
      });
    }
    return false;
  }
}

function onSave() {
  if (!validateForm()) {
    return;
  }
  // ... rest of save logic
}
```

**Template avec affichage des erreurs**
```vue
<UInput
  v-model="form.name"
  :error="validationErrors.name"
/>
<span v-if="validationErrors.name" class="text-red-500 text-sm">
  {{ validationErrors.name }}
</span>
```

---

### 4. Améliorer l'accessibilité

#### Ajouter ARIA aux checkboxes d'options

**`SponsoringPackForm.vue`**
```vue
<input
  :id="`required-${option.id}`"
  :checked="form.requiredOptions.includes(option.id)"
  type="checkbox"
  :value="option.id"
  :aria-labelledby="`label-required-${option.id}`"
  :aria-describedby="`price-required-${option.id}`"
  class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
  @change="handleRequiredOptionChange(option.id, ($event.target as HTMLInputElement).checked)"
/>
<label
  :id="`label-required-${option.id}`"
  :for="`required-${option.id}`"
  class="text-sm text-gray-700 cursor-pointer"
>
  {{ getOptionName(option) }}
  <span :id="`price-required-${option.id}`" class="sr-only">
    Prix: {{ option.price || 0 }} euros
  </span>
  ({{ option.price || 0 }}€)
</label>
```

#### Ajouter des régions ARIA live pour les messages

```vue
<div
  role="alert"
  aria-live="polite"
  aria-atomic="true"
  v-if="success"
  class="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4"
>
  {{ successMessage }}
</div>

<div
  role="alert"
  aria-live="assertive"
  aria-atomic="true"
  v-if="error"
  class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4"
>
  {{ error }}
</div>
```

#### Gérer le focus après actions

```typescript
async function onSave() {
  // ... save logic
  success.value = true;

  // Focus sur le message de succès
  await nextTick();
  const successElement = document.querySelector('[role="alert"]');
  if (successElement instanceof HTMLElement) {
    successElement.focus();
  }
}
```

#### Ajouter classe sr-only (screen reader only)

**`assets/css/main.css` ou dans `app.vue`**
```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
```

---

### 5. Implémenter Pinia pour State Management

#### Installation
```bash
pnpm add @pinia/nuxt
```

#### Configuration dans `nuxt.config.ts`
```typescript
export default defineNuxtConfig({
  modules: ['@nuxt/ui', '@nuxtjs/i18n', '@pinia/nuxt'],
  // ...
})
```

#### Créer les stores

**`stores/packs.ts`**
```typescript
import { defineStore } from 'pinia';
import { getOrgsEventsPacks, type SponsoringPack } from '~/utils/api';

export const usePacksStore = defineStore('packs', () => {
  const packs = ref<Record<string, SponsoringPack[]>>({});
  const loading = ref(false);
  const error = ref<string | null>(null);

  const getPacksByEvent = async (orgSlug: string, eventSlug: string) => {
    const key = `${orgSlug}-${eventSlug}`;

    // Retourner du cache si disponible
    if (packs.value[key]?.length > 0) {
      return packs.value[key];
    }

    loading.value = true;
    error.value = null;

    try {
      const response = await getOrgsEventsPacks(orgSlug, eventSlug);
      packs.value[key] = response.data;
      return response.data;
    } catch (err) {
      error.value = 'Failed to load packs';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const invalidateCache = (orgSlug: string, eventSlug: string) => {
    const key = `${orgSlug}-${eventSlug}`;
    delete packs.value[key];
  };

  return {
    packs,
    loading,
    error,
    getPacksByEvent,
    invalidateCache
  };
});
```

**`stores/options.ts`**
```typescript
import { defineStore } from 'pinia';
import { getOrgsEventsOptions, type SponsoringOption } from '~/utils/api';

export const useOptionsStore = defineStore('options', () => {
  const options = ref<Record<string, SponsoringOption[]>>({});
  const loading = ref(false);
  const error = ref<string | null>(null);

  const getOptionsByEvent = async (orgSlug: string, eventSlug: string) => {
    const key = `${orgSlug}-${eventSlug}`;

    if (options.value[key]?.length > 0) {
      return options.value[key];
    }

    loading.value = true;
    error.value = null;

    try {
      const response = await getOrgsEventsOptions(orgSlug, eventSlug);
      options.value[key] = response.data;
      return response.data;
    } catch (err) {
      error.value = 'Failed to load options';
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const invalidateCache = (orgSlug: string, eventSlug: string) => {
    const key = `${orgSlug}-${eventSlug}`;
    delete options.value[key];
  };

  return {
    options,
    loading,
    error,
    getOptionsByEvent,
    invalidateCache
  };
});
```

#### Utiliser dans les composants

**Exemple dans `sponsors/create.vue`**
```typescript
const packsStore = usePacksStore();
const optionsStore = useOptionsStore();

async function loadData() {
  try {
    loading.value = true;
    const [eventResponse] = await Promise.all([
      getEventBySlug(eventSlug.value),
      packsStore.getPacksByEvent(orgSlug.value, eventSlug.value)
    ]);
    eventName.value = eventResponse.data.event.name;
    packs.value = packsStore.packs[`${orgSlug.value}-${eventSlug.value}`] || [];
  } catch (err) {
    const { handleError } = useErrorHandler();
    error.value = handleError(err, 'loadData');
  } finally {
    loading.value = false;
  }
}
```

---

### 6. Ajouter Toast Notifications

**Créer `composables/useNotifications.ts`**
```typescript
export const useNotifications = () => {
  const toast = useToast();

  const showSuccess = (message: string, title = 'Succès') => {
    toast.add({
      title,
      description: message,
      color: 'green',
      icon: 'i-heroicons-check-circle',
      timeout: 3000
    });
  };

  const showError = (message: string, title = 'Erreur') => {
    toast.add({
      title,
      description: message,
      color: 'red',
      icon: 'i-heroicons-x-circle',
      timeout: 5000
    });
  };

  const showInfo = (message: string, title = 'Information') => {
    toast.add({
      title,
      description: message,
      color: 'blue',
      icon: 'i-heroicons-information-circle',
      timeout: 3000
    });
  };

  const showWarning = (message: string, title = 'Attention') => {
    toast.add({
      title,
      description: message,
      color: 'yellow',
      icon: 'i-heroicons-exclamation-triangle',
      timeout: 4000
    });
  };

  return {
    showSuccess,
    showError,
    showInfo,
    showWarning
  };
};
```

**Utiliser dans les composants**
```typescript
const { showSuccess, showError } = useNotifications();
const { handleError } = useErrorHandler();

async function onSave() {
  try {
    await postOrgsEventsPacks(orgSlug.value, eventSlug.value, packData);
    showSuccess('Pack créé avec succès !');
    router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`);
  } catch (err) {
    const errorMessage = handleError(err, 'onSave');
    showError(errorMessage);
  }
}
```

---

### 7. Compléter l'internationalisation

#### Ajouter clés manquantes dans les fichiers de traduction

**`locales/fr-FR.json`** - Ajouter toutes les clés de formulaires
```json
{
  "forms": {
    "required": "Requis",
    "optional": "Optionnel",
    "cancel": "Annuler",
    "validate": "Valider",
    "create": "Créer",
    "save": "Enregistrer",
    "delete": "Supprimer",
    "edit": "Modifier"
  },
  "packs": {
    "nameLabel": "Nom du pack",
    "priceLabel": "Prix (€)",
    "nbTicketsLabel": "Nombre de billets",
    "maxQuantityLabel": "Quantité maximale",
    "withBoothLabel": "Inclut un stand",
    "requiredOptionsLabel": "Options obligatoires",
    "optionalOptionsLabel": "Options optionnelles",
    "createSuccess": "Pack créé avec succès",
    "updateSuccess": "Pack mis à jour avec succès",
    "deleteConfirm": "Êtes-vous sûr de vouloir supprimer ce pack ?"
  },
  "options": {
    "nameLabel": "Nom de l'option",
    "descriptionLabel": "Description",
    "priceLabel": "Prix (€)",
    "languagesLabel": "Langues",
    "addLanguage": "Ajouter une langue",
    "createSuccess": "Option créée avec succès",
    "updateSuccess": "Option mise à jour avec succès"
  },
  "sponsors": {
    "companyNameLabel": "Nom de l'entreprise",
    "contactNameLabel": "Nom du contact",
    "contactRoleLabel": "Rôle du contact",
    "emailLabel": "Email",
    "phoneLabel": "Téléphone",
    "packLabel": "Pack de sponsoring",
    "optionsLabel": "Options",
    "createSuccess": "Sponsor créé avec succès",
    "createTitle": "Créer un sponsor"
  }
}
```

#### Remplacer dans les templates

**Avant :**
```vue
<label>Nom du pack*</label>
```

**Après :**
```vue
<label>{{ $t('packs.nameLabel') }}*</label>
```

---

### 8. Créer wizard multi-étapes pour sponsor

**`components/SponsorWizard.vue`**
```vue
<template>
  <div class="sponsor-wizard">
    <!-- Progress bar -->
    <div class="mb-8">
      <div class="flex justify-between mb-2">
        <span
          v-for="(step, index) in steps"
          :key="index"
          class="text-sm font-medium"
          :class="currentStep >= index ? 'text-primary-600' : 'text-gray-400'"
        >
          {{ step.title }}
        </span>
      </div>
      <div class="w-full bg-gray-200 rounded-full h-2">
        <div
          class="bg-primary-600 h-2 rounded-full transition-all duration-300"
          :style="{ width: `${(currentStep / (steps.length - 1)) * 100}%` }"
        />
      </div>
    </div>

    <!-- Step content -->
    <form @submit.prevent="handleNext">
      <!-- Step 1: Company Info -->
      <div v-show="currentStep === 0">
        <h2 class="text-xl font-semibold mb-4">Informations de l'entreprise</h2>
        <!-- Company fields -->
      </div>

      <!-- Step 2: Pack Selection -->
      <div v-show="currentStep === 1">
        <h2 class="text-xl font-semibold mb-4">Sélection du pack</h2>
        <!-- Pack selection -->
      </div>

      <!-- Step 3: Options Selection -->
      <div v-show="currentStep === 2">
        <h2 class="text-xl font-semibold mb-4">Options supplémentaires</h2>
        <!-- Options checkboxes -->
      </div>

      <!-- Step 4: Summary -->
      <div v-show="currentStep === 3">
        <h2 class="text-xl font-semibold mb-4">Récapitulatif</h2>
        <!-- Summary -->
      </div>

      <!-- Navigation buttons -->
      <div class="flex justify-between mt-8">
        <UButton
          v-if="currentStep > 0"
          type="button"
          color="gray"
          @click="currentStep--"
        >
          Précédent
        </UButton>
        <div v-else />

        <UButton
          v-if="currentStep < steps.length - 1"
          type="submit"
          color="primary"
        >
          Suivant
        </UButton>
        <UButton
          v-else
          type="button"
          color="primary"
          :loading="saving"
          @click="handleSubmit"
        >
          Créer le sponsor
        </UButton>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
const currentStep = ref(0);
const saving = ref(false);

const steps = [
  { title: 'Entreprise', valid: ref(false) },
  { title: 'Pack', valid: ref(false) },
  { title: 'Options', valid: ref(false) },
  { title: 'Récapitulatif', valid: ref(false) }
];

const formData = ref({
  // ... all form fields
});

function handleNext() {
  // Validate current step
  if (validateStep(currentStep.value)) {
    currentStep.value++;
  }
}

function validateStep(step: number): boolean {
  // Implement validation logic per step
  return true;
}

async function handleSubmit() {
  // Final submission
}
</script>
```

---

### 9. Ajouter Breadcrumbs

**`components/Breadcrumbs.vue`**
```vue
<template>
  <nav aria-label="Breadcrumb" class="mb-4">
    <ol class="flex items-center space-x-2 text-sm">
      <li v-for="(crumb, index) in breadcrumbs" :key="index" class="flex items-center">
        <NuxtLink
          v-if="index < breadcrumbs.length - 1"
          :to="crumb.to"
          class="text-gray-600 hover:text-gray-900 transition-colors"
        >
          {{ crumb.label }}
        </NuxtLink>
        <span v-else class="text-gray-900 font-medium">
          {{ crumb.label }}
        </span>
        <svg
          v-if="index < breadcrumbs.length - 1"
          class="w-4 h-4 mx-2 text-gray-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
        </svg>
      </li>
    </ol>
  </nav>
</template>

<script setup lang="ts">
interface Breadcrumb {
  label: string;
  to: string;
}

const route = useRoute();

const breadcrumbs = computed<Breadcrumb[]>(() => {
  const crumbs: Breadcrumb[] = [
    { label: 'Accueil', to: '/orgs' }
  ];

  const pathSegments = route.path.split('/').filter(Boolean);

  // Build breadcrumbs from route
  if (pathSegments[0] === 'orgs') {
    const orgSlug = pathSegments[1];
    if (orgSlug && orgSlug !== 'create') {
      crumbs.push({ label: 'Organisation', to: `/orgs/${orgSlug}` });

      if (pathSegments[2] === 'events') {
        const eventSlug = pathSegments[3];
        if (eventSlug && eventSlug !== 'create') {
          crumbs.push({ label: 'Événement', to: `/orgs/${orgSlug}/events/${eventSlug}` });

          if (pathSegments[4] === 'packs') {
            crumbs.push({ label: 'Packs', to: `/orgs/${orgSlug}/events/${eventSlug}/packs` });
          } else if (pathSegments[4] === 'options') {
            crumbs.push({ label: 'Options', to: `/orgs/${orgSlug}/events/${eventSlug}/options` });
          } else if (pathSegments[4] === 'sponsors') {
            crumbs.push({ label: 'Sponsors', to: `/orgs/${orgSlug}/events/${eventSlug}/sponsors` });
          }
        }
      }
    }
  }

  return crumbs;
});
</script>
```

**Utiliser dans `Dashboard.vue`**
```vue
<template>
  <div class="dashboard">
    <Breadcrumbs />
    <!-- rest of dashboard -->
  </div>
</template>
```

---

### 10. Ajouter recherche/filtrage sur tableaux

**Exemple pour packs (`packs/index.vue`)**
```vue
<template>
  <div>
    <!-- Search bar -->
    <div class="mb-4">
      <UInput
        v-model="searchQuery"
        placeholder="Rechercher un pack..."
        icon="i-heroicons-magnifying-glass"
      />
    </div>

    <!-- Table -->
    <UTable
      :data="filteredPacks"
      :columns="columns"
      @select="onSelectPack"
    />

    <!-- Results count -->
    <p class="mt-2 text-sm text-gray-600">
      {{ filteredPacks.length }} résultat(s) sur {{ packs.length }}
    </p>
  </div>
</template>

<script setup lang="ts">
const searchQuery = ref('');

const filteredPacks = computed(() => {
  if (!searchQuery.value) return packs.value;

  const query = searchQuery.value.toLowerCase();
  return packs.value.filter(pack =>
    pack.name.toLowerCase().includes(query) ||
    pack.base_price.toString().includes(query)
  );
});
</script>
```

---

### 11. Optimistic UI

**Exemple lors de la création d'un pack**
```typescript
async function onSave(data: { pack: CreateSponsoringPack; ... }) {
  const { showSuccess, showError } = useNotifications();
  const packsStore = usePacksStore();

  // Créer un ID temporaire
  const tempId = `temp-${Date.now()}`;
  const optimisticPack = {
    ...data.pack,
    id: tempId,
    // autres champs avec valeurs par défaut
  };

  try {
    // Ajouter immédiatement à la liste (optimistic)
    const key = `${orgSlug.value}-${eventSlug.value}`;
    if (packsStore.packs[key]) {
      packsStore.packs[key].push(optimisticPack as any);
    }

    // Rediriger immédiatement
    router.push(`/orgs/${orgSlug.value}/events/${eventSlug.value}/packs`);

    // Faire l'appel API en arrière-plan
    const response = await postOrgsEventsPacks(orgSlug.value, eventSlug.value, data.pack);

    // Remplacer l'élément temporaire par le vrai
    if (packsStore.packs[key]) {
      const index = packsStore.packs[key].findIndex(p => p.id === tempId);
      if (index !== -1) {
        packsStore.packs[key][index] = response.data;
      }
    }

    showSuccess('Pack créé avec succès');
  } catch (err) {
    // Rollback: retirer l'élément optimistic
    if (packsStore.packs[key]) {
      packsStore.packs[key] = packsStore.packs[key].filter(p => p.id !== tempId);
    }

    const { handleError } = useErrorHandler();
    const errorMessage = handleError(err, 'onSave');
    showError(errorMessage);
  }
}
```

---

### 12. Guard de navigation pour formulaires

**Ajouter dans les pages de formulaire**
```typescript
// Dans setup()
const formDirty = ref(false);

// Surveiller les changements
watch(formData, () => {
  formDirty.value = true;
}, { deep: true });

// Guard de navigation
onBeforeRouteLeave((to, from, next) => {
  if (formDirty.value && !success.value) {
    const answer = window.confirm(
      'Vous avez des modifications non sauvegardées. Voulez-vous vraiment quitter ?'
    );
    if (answer) {
      next();
    } else {
      next(false);
    }
  } else {
    next();
  }
});

// Réinitialiser après sauvegarde
function onSave() {
  // ... save logic
  formDirty.value = false;
}
```

---

### 13. Refactoriser handlers d'options

**Créer une fonction générique**
```typescript
function createMutuallyExclusiveHandler(
  listA: Ref<string[]>,
  listB: Ref<string[]>
) {
  return (itemId: string, checked: boolean, targetListIsA: boolean) => {
    const targetList = targetListIsA ? listA : listB;
    const oppositeList = targetListIsA ? listB : listA;

    if (checked) {
      // Ajouter à la liste cible
      if (!targetList.value.includes(itemId)) {
        targetList.value.push(itemId);
      }
      // Retirer de la liste opposée
      oppositeList.value = oppositeList.value.filter(id => id !== itemId);
    } else {
      // Retirer de la liste cible
      targetList.value = targetList.value.filter(id => id !== itemId);
    }
  };
}

// Utilisation
const requiredOptions = ref<string[]>([]);
const optionalOptions = ref<string[]>([]);

const handleOptionChange = createMutuallyExclusiveHandler(
  requiredOptions,
  optionalOptions
);

// Dans le template
@change="handleOptionChange(option.id, $event.target.checked, true)" // pour required
@change="handleOptionChange(option.id, $event.target.checked, false)" // pour optional
```

---

## 📋 Checklist d'implémentation

- [ ] Installer Zod et créer schémas de validation
- [ ] Ajouter validation dans tous les formulaires
- [ ] Améliorer accessibilité (ARIA, focus, sr-only)
- [ ] Installer et configurer Pinia
- [ ] Créer stores pour packs, options, events
- [ ] Migrer appels API vers stores
- [ ] Implémenter toast notifications
- [ ] Compléter i18n (extraire textes en dur)
- [ ] Créer wizard multi-étapes pour sponsors
- [ ] Ajouter breadcrumbs dans Dashboard
- [ ] Ajouter recherche/filtrage sur tableaux
- [ ] Implémenter Optimistic UI
- [ ] Ajouter guards de navigation
- [ ] Refactoriser handlers d'options
- [ ] Tester responsive sur mobile
- [ ] Vérifier contrastes de couleurs

---

## 🎯 Priorités

### Haute priorité (faire en premier)
1. Validation Zod
2. Toast notifications
3. Accessibilité ARIA de base
4. Pinia stores

### Moyenne priorité
5. i18n complet
6. Breadcrumbs
7. Recherche/filtrage
8. Wizard sponsor

### Basse priorité (nice to have)
9. Optimistic UI
10. Guards navigation
11. Refactoring handlers

---

Ce document devrait être consulté et mis à jour au fur et à mesure de l'implémentation.
