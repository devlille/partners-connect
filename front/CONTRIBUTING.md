# Guide de contribution - Partners Connect Frontend

> Bienvenue dans le projet Partners Connect! Ce document explique comment contribuer au projet de manière efficace et cohérente.

## Table des matières

1. [Mise en route](#mise-en-route)
2. [Conventions de code](#conventions-de-code)
3. [Architecture du projet](#architecture-du-projet)
4. [Workflow Git](#workflow-git)
5. [Tests](#tests)
6. [Documentation](#documentation)
7. [Revue de code](#revue-de-code)
8. [Ressources](#ressources)

---

## Mise en route

### Prérequis

- **Node.js**: 20.x ou supérieur
- **npm**: 10.x ou supérieur
- **Git**: Pour le contrôle de version

### Installation

```bash
# Cloner le repository
git clone https://github.com/your-org/partners-connect-front.git
cd partners-connect-front

# Installer les dépendances
npm install

# Générer le client API depuis OpenAPI
npm run orval

# Lancer le serveur de développement
npm run dev
```

L'application sera accessible sur `http://localhost:3000`

### Variables d'environnement

Créer un fichier `.env` à la racine avec:

```env
NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

## Conventions de code

### 1. Nommage

#### Fichiers et dossiers

| Type | Convention | Exemples |
|------|------------|----------|
| **Pages** | kebab-case | `user-profile.vue`, `event-details.vue` |
| **Composants** | PascalCase | `UserProfile.vue`, `EventCard.vue` |
| **Composables** | camelCase avec `use` | `useAuth.ts`, `useEventLinks.ts` |
| **Utils** | camelCase | `formatDate.ts`, `validateEmail.ts` |
| **Stores** | camelCase avec `use` et `Store` | `useAuthStore.ts`, `usePacksStore.ts` |
| **Types** | camelCase | `user.ts`, `event.ts`, `form.ts` |
| **Constants** | kebab-case | `routes.ts`, `storage-keys.ts` |

#### Variables et fonctions

```typescript
// Variables: camelCase
const userName = 'John'
const isLoading = false
const totalCount = 42

// Constantes: UPPER_SNAKE_CASE
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
const API_TIMEOUT = 30000
const DEFAULT_LOCALE = 'fr'

// Fonctions: camelCase avec verbe d'action
function fetchUserData() { }
function validateForm() { }
function handleSubmit() { }

// Composables: camelCase avec préfixe "use"
function useAuth() { }
function useModal() { }

// Composants: PascalCase
const UserProfile = defineComponent({ })
const EventCard = defineComponent({ })

// Types/Interfaces: PascalCase
interface UserProfile { }
type EventStatus = 'draft' | 'published'
```

---

### 2. Structure de fichier Vue

Ordre standard pour les fichiers `.vue`:

```vue
<template>
  <!-- 1. Template HTML -->
</template>

<script setup lang="ts">
// 2. Imports groupés
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getEventBySlug } from '~/utils/api'

// 3. Types et interfaces
interface FormData {
  name: string
  email: string
}

// 4. Props et emits
const props = defineProps<{
  eventId: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  submit: [data: FormData]
  cancel: []
}>()

// 5. Composables
const route = useRoute()
const { t } = useI18n()

// 6. State (ref, reactive)
const loading = ref(false)
const formData = reactive<FormData>({
  name: '',
  email: ''
})

// 7. Computed
const isValid = computed(() => {
  return formData.name.length > 0 && formData.email.includes('@')
})

// 8. Methods
async function handleSubmit() {
  // Logic here
}

function resetForm() {
  // Logic here
}

// 9. Lifecycle hooks
onMounted(() => {
  // Init logic
})

// 10. Watchers (si nécessaire)
watch(() => props.eventId, (newId) => {
  // React to changes
})
</script>

<style scoped>
/* 11. Styles scoped si nécessaire */
/* Privilégier Tailwind CSS */
</style>
```

---

### 3. TypeScript

#### Types stricts

```typescript
// ✅ BON: Types explicites
const user: UserProfile = {
  id: '123',
  name: 'John Doe',
  email: 'john@example.com'
}

function getUserById(id: string): Promise<UserProfile> {
  return fetchUser(id)
}

// ❌ MAUVAIS: any
const data: any = fetchData()
function process(input: any) { }

// ✅ BON: Type générique ou unknown
const data: unknown = fetchData()
function process<T>(input: T): T { }
```

#### Props typées

```typescript
// ✅ BON: Props avec interface
interface Props {
  title: string
  count?: number
  onUpdate?: (value: string) => void
}

const props = defineProps<Props>()

// ❌ MAUVAIS: Props sans types
const props = defineProps({
  title: String,
  count: Number
})
```

#### Inférence de types avec Zod

```typescript
import { z } from 'zod'

// Schéma de validation
const userSchema = z.object({
  name: z.string().min(2),
  email: z.string().email(),
  age: z.number().positive().optional()
})

// Inférence du type TypeScript
type User = z.infer<typeof userSchema>

// Validation
const result = userSchema.safeParse(data)
if (result.success) {
  const user: User = result.data
}
```

---

### 4. Composants

#### Petits et focalisés

```typescript
// ✅ BON: Composant simple avec une responsabilité
// components/UserAvatar.vue
<template>
  <img :src="src" :alt="alt" class="w-10 h-10 rounded-full" />
</template>

<script setup lang="ts">
const props = defineProps<{
  src: string
  alt: string
}>()
</script>

// ❌ MAUVAIS: Composant qui fait trop de choses (>300 lignes)
```

**Règle**: Maximum 200 lignes par composant. Si plus grand, découper en sous-composants.

#### Émission d'événements

```typescript
// ✅ BON: Émissions typées
const emit = defineEmits<{
  update: [value: string]
  submit: [data: FormData]
  cancel: []
}>()

emit('update', 'new value')
emit('submit', formData)
emit('cancel')

// ❌ MAUVAIS: Émissions non typées
const emit = defineEmits(['update', 'submit'])
emit('update', someValue)
```

#### Slots

```vue
<!-- ✅ BON: Slots nommés avec fallback -->
<template>
  <div class="card">
    <div class="card-header">
      <slot name="header">
        <h3>{{ title }}</h3>
      </slot>
    </div>
    <div class="card-body">
      <slot>Default content</slot>
    </div>
    <div class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>
```

---

### 5. Composables

#### Structure standard

```typescript
// composables/useEventData.ts
export function useEventData(eventSlug: string) {
  // State
  const event = ref<Event | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Computed
  const isUpcoming = computed(() => {
    if (!event.value) return false
    return new Date(event.value.start_date) > new Date()
  })

  // Methods
  async function loadEvent() {
    loading.value = true
    error.value = null
    try {
      const response = await getEventBySlug(eventSlug)
      event.value = response.data.event
    } catch (err) {
      error.value = 'Failed to load event'
      console.error(err)
    } finally {
      loading.value = false
    }
  }

  // Auto-load on mount
  onMounted(() => {
    loadEvent()
  })

  // Return API
  return {
    // State
    event: readonly(event),
    loading: readonly(loading),
    error: readonly(error),
    // Computed
    isUpcoming,
    // Methods
    loadEvent,
    refresh: loadEvent
  }
}
```

#### Réutilisation

```typescript
// ✅ BON: Logique extraite dans un composable
// composables/usePartnerships.ts
export function usePartnerships(orgSlug: string, eventSlug: string) {
  const partnerships = ref<Partnership[]>([])
  const loading = ref(false)

  async function loadPartnerships() {
    loading.value = true
    try {
      const { data } = await getOrgsEventsPartnership(orgSlug, eventSlug)
      partnerships.value = data
    } finally {
      loading.value = false
    }
  }

  const activePartnerships = computed(() =>
    partnerships.value.filter(p => p.status === 'active')
  )

  return { partnerships, loading, loadPartnerships, activePartnerships }
}

// Utilisation dans un composant
const { partnerships, loading, loadPartnerships } = usePartnerships(orgSlug, eventSlug)
```

---

### 6. État et réactivité

#### Refs vs Reactive

```typescript
// ✅ BON: ref pour les primitives et objets simples
const count = ref(0)
const user = ref<User | null>(null)
const isLoading = ref(false)

// ✅ BON: reactive pour les objets complexes
const formData = reactive({
  name: '',
  email: '',
  preferences: {
    newsletter: false,
    notifications: true
  }
})

// ⚠️ ATTENTION: reactive perd sa réactivité lors de la déstructuration
const { name, email } = formData // name et email ne sont plus réactifs!

// ✅ BON: Utiliser toRefs pour déstructurer
const { name, email } = toRefs(formData) // Maintenant réactifs
```

#### Computed

```typescript
// ✅ BON: Pour les transformations de données
const fullName = computed(() => `${firstName.value} ${lastName.value}`)
const filteredItems = computed(() =>
  items.value.filter(item => item.isActive)
)

// ❌ MAUVAIS: Logique lourde dans le template
<template>
  <div>{{ items.filter(i => i.isActive).map(i => i.name).join(', ') }}</div>
</template>

// ✅ BON: Utiliser computed
const activeItemNames = computed(() =>
  items.value.filter(i => i.isActive).map(i => i.name).join(', ')
)

<template>
  <div>{{ activeItemNames }}</div>
</template>
```

---

### 7. Gestion d'erreurs

#### Pattern standard

```typescript
// composables/useErrorHandler.ts déjà disponible
import { useErrorHandler } from '~/composables/useErrorHandler'

const { handleError } = useErrorHandler()

async function submitForm() {
  try {
    loading.value = true
    await saveData(formData)
    toast.add({ title: 'Succès', color: 'green' })
  } catch (error) {
    handleError(error) // Gestion centralisée
  } finally {
    loading.value = false
  }
}
```

#### Messages d'erreur

```typescript
// ✅ BON: Messages explicites et actionnables
const ERROR_MESSAGES = {
  NETWORK: 'Impossible de se connecter au serveur. Vérifiez votre connexion.',
  UNAUTHORIZED: 'Session expirée. Veuillez vous reconnecter.',
  VALIDATION: 'Certains champs sont invalides. Corrigez-les et réessayez.',
  NOT_FOUND: 'La ressource demandée est introuvable.'
}

// ❌ MAUVAIS: Messages techniques
throw new Error('ERR_CONNECTION_REFUSED')
throw new Error('500 Internal Server Error')
```

---

### 8. API et requêtes

#### Appels API

```typescript
// ✅ BON: Appels parallèles quand indépendants
const [event, packs, partnerships] = await Promise.all([
  getEventBySlug(eventSlug),
  getOrgsEventsPacks(orgSlug, eventSlug),
  getOrgsEventsPartnership(orgSlug, eventSlug)
])

// ❌ MAUVAIS: Appels séquentiels inutiles
const event = await getEventBySlug(eventSlug)
const packs = await getOrgsEventsPacks(orgSlug, eventSlug)
const partnerships = await getOrgsEventsPartnership(orgSlug, eventSlug)
```

#### Utilisation du cache

```typescript
import { useCache } from '~/composables/useCache'

const cache = useCache()

// Données qui changent rarement
const packs = await cache.fetchWithCache(
  `packs-${eventSlug}`,
  () => getOrgsEventsPacks(orgSlug, eventSlug),
  { ttl: 15 * 60 * 1000 } // 15 minutes
)

// Invalider le cache après modification
await createPack(packData)
cache.invalidate(`packs-${eventSlug}`)
```

---

### 9. Styles et CSS

#### Tailwind CSS (prioritaire)

```vue
<!-- ✅ BON: Utiliser Tailwind -->
<template>
  <div class="flex items-center gap-4 p-6 bg-white rounded-lg shadow">
    <img :src="avatar" class="w-12 h-12 rounded-full" />
    <div class="flex-1">
      <h3 class="text-lg font-semibold text-gray-900">{{ name }}</h3>
      <p class="text-sm text-gray-500">{{ email }}</p>
    </div>
  </div>
</template>

<!-- ❌ MAUVAIS: CSS custom pour des choses standard -->
<style scoped>
.card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
</style>
```

#### Classes conditionnelles

```vue
<template>
  <!-- ✅ BON: Array syntax pour conditions -->
  <button
    :class="[
      'px-4 py-2 rounded font-medium',
      isActive ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700',
      disabled && 'opacity-50 cursor-not-allowed'
    ]"
  >
    {{ label }}
  </button>
</template>
```

---

### 10. Internationalisation (i18n)

#### Utilisation

```vue
<script setup>
const { t } = useI18n()
</script>

<template>
  <!-- ✅ BON: Utiliser i18n pour tous les textes -->
  <h1>{{ t('pages.events.title') }}</h1>
  <p>{{ t('pages.events.description') }}</p>

  <!-- Avec paramètres -->
  <p>{{ t('common.greeting', { name: userName }) }}</p>

  <!-- Pluralisation -->
  <p>{{ t('common.items', { count: itemCount }) }}</p>

  <!-- ❌ MAUVAIS: Texte en dur -->
  <h1>Liste des événements</h1>
</template>
```

#### Fichiers de traduction

```json
// locales/fr-FR.json
{
  "pages": {
    "events": {
      "title": "Événements",
      "description": "Gérez vos événements",
      "create": "Créer un événement"
    }
  },
  "common": {
    "greeting": "Bonjour {name}",
    "items": "{count} élément | {count} éléments",
    "save": "Enregistrer",
    "cancel": "Annuler"
  }
}
```

---

### 11. Accessibilité

#### Bonnes pratiques

```vue
<template>
  <!-- ✅ BON: Labels et ARIA -->
  <button
    aria-label="Fermer le modal"
    @click="close"
  >
    <i class="i-heroicons-x-mark" aria-hidden="true" />
  </button>

  <!-- ✅ BON: Label visible pour les inputs -->
  <div>
    <label for="email" class="block text-sm font-medium">
      Email
    </label>
    <input
      id="email"
      v-model="email"
      type="email"
      aria-describedby="email-error"
    />
    <p id="email-error" class="text-sm text-red-600">
      {{ emailError }}
    </p>
  </div>

  <!-- ✅ BON: Navigation clavier -->
  <div
    role="dialog"
    aria-modal="true"
    aria-labelledby="modal-title"
  >
    <h2 id="modal-title">{{ title }}</h2>
    <!-- Contenu -->
  </div>
</template>
```

---

## Architecture du projet

### Structure des dossiers

```
/front
├── pages/              # Routes automatiques Nuxt
│   ├── index.vue      # Page d'accueil
│   ├── login.vue      # Login
│   └── orgs/          # Organisations (routes dynamiques)
├── components/         # Composants réutilisables
│   ├── ui/            # Composants UI de base
│   ├── forms/         # Composants de formulaire
│   └── layout/        # Layout components
├── composables/        # Logique réutilisable
│   ├── useAuth.ts
│   ├── useCache.ts
│   └── useModal.ts
├── stores/            # Pinia stores
│   ├── auth.ts
│   ├── packs.ts
│   └── sponsors.ts
├── utils/             # Fonctions utilitaires
│   ├── api.ts         # Client API (généré)
│   └── validation/    # Validations Zod
├── types/             # Types TypeScript
│   ├── user.ts
│   ├── event.ts
│   └── form.ts
├── constants/         # Constantes
│   ├── routes.ts
│   ├── ui.ts
│   └── errors.ts
├── middleware/        # Middleware de route
│   └── auth.ts
├── locales/           # Fichiers i18n
│   ├── fr-FR.json
│   ├── en-US.json
│   └── es-ES.json
└── tests/             # Tests unitaires/e2e
    └── composables/
```

### Patterns de routing

```typescript
// ✅ BON: Routes dynamiques avec slugs
/orgs/[slug]/events/[eventSlug]/packs/[packId]

// Navigation programmatique
const router = useRouter()
router.push(`/orgs/${orgSlug}/events/${eventSlug}/packs`)

// Avec paramètres de query
router.push({
  path: '/search',
  query: { q: searchTerm, page: 1 }
})
```

---

## Workflow Git

### Branches

```bash
# Format des noms de branches
feature/add-user-profile
fix/login-error
refactor/api-client
docs/update-contributing

# Branches principales
main          # Production
develop       # Développement (si utilisé)
```

### Commits

#### Format

Utiliser le format Conventional Commits:

```
<type>(<scope>): <description>

[corps optionnel]

[footer optionnel]
```

**Types**:
- `feat`: Nouvelle fonctionnalité
- `fix`: Correction de bug
- `refactor`: Refactoring sans changement de fonctionnalité
- `style`: Changements de style (formatting, etc.)
- `docs`: Documentation
- `test`: Ajout/modification de tests
- `chore`: Tâches de maintenance
- `perf`: Amélioration de performance

**Exemples**:

```bash
feat(auth): add Google OAuth login
fix(sponsors): correct pagination on sponsors list
refactor(api): extract error handling to composable
docs(contributing): add TypeScript conventions
test(useModal): add unit tests for modal composable
chore(deps): update Nuxt to 4.1.0
perf(cache): implement memoization for expensive computations
```

#### Messages

```bash
# ✅ BON: Description claire et concise
git commit -m "feat(sponsors): add filter by pack type"
git commit -m "fix(modal): prevent modal from showing on page load"
git commit -m "refactor(forms): extract validation logic to composable"

# ❌ MAUVAIS: Messages vagues
git commit -m "fix bugs"
git commit -m "update"
git commit -m "WIP"
```

### Pull Requests

#### Template

```markdown
## Description
Brève description des changements

## Type de changement
- [ ] Bug fix
- [ ] Nouvelle fonctionnalité
- [ ] Refactoring
- [ ] Documentation
- [ ] Performance

## Checklist
- [ ] Code suit les conventions du projet
- [ ] Tests ajoutés/mis à jour
- [ ] Documentation mise à jour
- [ ] Pas de warnings ESLint
- [ ] Tests passent localement
- [ ] Testé manuellement

## Captures d'écran (si applicable)
[Ajouter des captures]

## Notes pour les reviewers
Éléments particuliers à vérifier
```

---

## Tests

### Tests unitaires

#### Composables

```typescript
// tests/composables/useAuth.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { useAuth } from '~/composables/useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('should initialize with no user', () => {
    const { user, isAuthenticated } = useAuth()
    expect(user.value).toBeNull()
    expect(isAuthenticated.value).toBe(false)
  })

  it('should login successfully', async () => {
    const { login, user, isAuthenticated } = useAuth()
    await login('fake-token')

    expect(user.value).not.toBeNull()
    expect(isAuthenticated.value).toBe(true)
  })

  it('should logout and clear user data', () => {
    const { login, logout, user, isAuthenticated } = useAuth()
    login('fake-token')
    logout()

    expect(user.value).toBeNull()
    expect(isAuthenticated.value).toBe(false)
  })
})
```

#### Composants

```typescript
// tests/components/UserCard.test.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import UserCard from '~/components/UserCard.vue'

describe('UserCard', () => {
  it('renders user information', () => {
    const wrapper = mount(UserCard, {
      props: {
        user: {
          name: 'John Doe',
          email: 'john@example.com',
          avatar: 'https://example.com/avatar.jpg'
        }
      }
    })

    expect(wrapper.text()).toContain('John Doe')
    expect(wrapper.text()).toContain('john@example.com')
    expect(wrapper.find('img').attributes('src')).toBe('https://example.com/avatar.jpg')
  })

  it('emits click event when clicked', async () => {
    const wrapper = mount(UserCard, {
      props: { user: { name: 'John', email: 'john@example.com' } }
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
```

### Couverture de tests

```bash
# Exécuter les tests
npm run test

# Avec UI interactive
npm run test:ui

# Générer le rapport de couverture
npm run test:coverage
```

**Objectif**: Maintenir une couverture > 80%

---

## Documentation

### JSDoc pour les fonctions publiques

```typescript
/**
 * Formate une date selon la locale actuelle
 *
 * @param date - La date à formater
 * @param format - Le format désiré ('short' | 'long' | 'full')
 * @returns La date formatée en string
 *
 * @example
 * formatDate(new Date(), 'short') // '25/10/2025'
 * formatDate(new Date(), 'long') // '25 octobre 2025'
 */
export function formatDate(
  date: Date,
  format: 'short' | 'long' | 'full' = 'short'
): string {
  // Implementation
}
```

### README de composables

```typescript
// composables/useCache.ts

/**
 * # useCache
 *
 * Composable pour la gestion du cache avec TTL
 *
 * ## Utilisation
 *
 * ```typescript
 * const cache = useCache()
 *
 * // Récupérer avec cache
 * const data = await cache.fetchWithCache(
 *   'my-key',
 *   () => fetchData(),
 *   { ttl: 5 * 60 * 1000 } // 5 minutes
 * )
 *
 * // Invalider
 * cache.invalidate('my-key')
 *
 * // Nettoyer tout
 * cache.clear()
 * ```
 *
 * ## Features
 * - TTL automatique
 * - Persistence localStorage
 * - Pattern matching pour invalidation
 */
export function useCache() {
  // ...
}
```

---

## Revue de code

### Checklist du reviewer

- [ ] **Code Quality**
  - [ ] Suit les conventions de nommage
  - [ ] Pas de code dupliqué
  - [ ] Pas de magic numbers
  - [ ] Commentaires utiles (pas d'évidence)

- [ ] **TypeScript**
  - [ ] Pas de `any`
  - [ ] Types appropriés
  - [ ] Inférence de types utilisée

- [ ] **Performance**
  - [ ] Pas de re-renders inutiles
  - [ ] Computed pour transformations
  - [ ] Debounce sur les inputs

- [ ] **Accessibilité**
  - [ ] Labels présents
  - [ ] ARIA attributes
  - [ ] Navigation clavier

- [ ] **Tests**
  - [ ] Tests unitaires ajoutés
  - [ ] Tests passent
  - [ ] Couverture maintenue

- [ ] **Documentation**
  - [ ] JSDoc pour fonctions publiques
  - [ ] README mis à jour si nécessaire

### Feedback constructif

```markdown
# ✅ BON
> Sur la ligne 42, vous pourriez utiliser `computed` au lieu de recalculer dans le template pour améliorer les performances.

# ❌ MAUVAIS
> Ce code est mauvais, refaites-le.
```

---

## Ressources

### Documentation officielle

- **Nuxt 3**: https://nuxt.com/docs
- **Vue 3**: https://vuejs.org/guide/
- **TypeScript**: https://www.typescriptlang.org/docs/
- **Pinia**: https://pinia.vuejs.org/
- **Tailwind CSS**: https://tailwindcss.com/docs
- **Vitest**: https://vitest.dev/guide/

### Outils

- **ESLint**: Linting automatique
- **Orval**: Génération client API
- **Zod**: Validation de schémas
- **i18n**: Internationalisation

### Commandes utiles

```bash
# Développement
npm run dev                    # Démarrer le serveur de dev
npm run build                  # Build pour production
npm run preview                # Preview du build

# Qualité
npm run lint                   # Linter le code
npm run test                   # Exécuter les tests
npm run test:coverage          # Rapport de couverture

# API
npm run orval                  # Régénérer le client API
```

---

## Questions fréquentes

### Comment ajouter une nouvelle page?

1. Créer un fichier dans `/pages/` (Nuxt créera automatiquement la route)
2. Ajouter le middleware auth si nécessaire
3. Implémenter le composant
4. Ajouter les traductions i18n
5. Ajouter les tests

### Comment créer un nouveau composable?

1. Créer le fichier dans `/composables/useMyFeature.ts`
2. Suivre le pattern standard (state, computed, methods, return)
3. Ajouter JSDoc
4. Écrire les tests unitaires
5. Utiliser dans les composants

### Comment déboguer?

```typescript
// Vue DevTools dans le navigateur
// Console logs (retirer avant commit)
console.log('Debug:', value)

// Débogueur
debugger

// Vue warn pour les problèmes Vue
import { warn } from 'vue'
warn('Something is wrong')
```

### Comment optimiser les performances?

1. Utiliser `computed` pour les transformations
2. `shallowRef`/`shallowReactive` pour les gros objets
3. Lazy loading des routes avec `defineAsyncComponent`
4. Virtualisation pour les longues listes
5. Debounce pour les inputs
6. Cache avec `useCache`

---

## Obtenir de l'aide

- **Issues GitHub**: Pour les bugs et features
- **Discussions**: Pour les questions générales
- **Code Review**: Demander sur votre PR

---

**Merci de contribuer au projet Partners Connect!** 🚀

> Dernière mise à jour: 25 octobre 2025
