# UrlInput Component

Composant de saisie dédié pour les URLs avec validation automatique du format.

## Caractéristiques

- ✅ **Validation automatique** : Vérifie que l'URL est valide (commence par http:// ou https://)
- 🔒 **Option HTTPS uniquement** : Peut forcer l'utilisation de HTTPS uniquement
- 🔴 **Messages d'erreur** : Affiche des messages d'erreur clairs et traduits
- 📱 **Type natif** : Utilise `type="url"` pour une meilleure expérience mobile
- ♿ **Accessible** : Labels et messages d'erreur associés correctement

## Validation

Le composant valide le format URL selon la regex définie dans `constants/validation.ts` :

```typescript
URL_REGEX = /^https?:\/\/.+/
```

L'URL doit commencer par `http://` ou `https://`.

## Messages d'erreur

Les messages d'erreur sont définis dans `constants/validation.ts` :

- **Format invalide** : "URL invalide"
- **HTTP non autorisé** : "URL doit utiliser HTTPS (https://)" (si `allowHttp=false`)
- **Champ requis** : "Ce champ est requis" (si `required=true`)

## Utilisation de base

```vue
<template>
  <UrlInput v-model="company.site_url" />
</template>

<script setup lang="ts">
const company = ref({
  site_url: ''
});
</script>
```

## Props

| Prop | Type | Défaut | Description |
|------|------|--------|-------------|
| `modelValue` | `string \| null \| undefined` | - | Valeur du champ (v-model) |
| `label` | `string` | `'URL'` | Texte du label |
| `placeholder` | `string` | `'https://example.com'` | Texte du placeholder |
| `disabled` | `boolean` | `false` | Désactive le champ |
| `required` | `boolean` | `false` | Marque le champ comme requis |
| `hint` | `string` | - | Texte d'aide sous le champ |
| `inputClass` | `string` | `'w-full'` | Classes CSS additionnelles |
| `validate` | `boolean` | `true` | Active/désactive la validation |
| `showErrorOnInput` | `boolean` | `false` | Affiche l'erreur pendant la saisie |
| `allowHttp` | `boolean` | `true` | Autorise HTTP (sinon uniquement HTTPS) |

## Événements

| Événement | Payload | Description |
|-----------|---------|-------------|
| `update:modelValue` | `string \| null` | Émis quand la valeur change |
| `validation` | `boolean` | Émis quand l'état de validation change |

## Exemples d'utilisation

### Champ requis

```vue
<UrlInput
  v-model="form.site_url"
  :required="true"
/>
```

### HTTPS uniquement

```vue
<UrlInput
  v-model="form.site_url"
  :allow-http="false"
  label="Site web (HTTPS requis)"
/>
```

### Avec texte d'aide personnalisé

```vue
<UrlInput
  v-model="form.site_url"
  hint="L'URL de votre site web principal"
/>
```

### Avec label et placeholder personnalisés

```vue
<UrlInput
  v-model="form.rib_url"
  label="RIB (URL)"
  placeholder="https://..."
/>
```

### Avec validation en temps réel

```vue
<UrlInput
  v-model="form.site_url"
  :show-error-on-input="true"
/>
```

### Désactivé

```vue
<UrlInput
  v-model="form.site_url"
  :disabled="isLoading"
/>
```

### Sans validation (formulaire dynamique)

```vue
<UrlInput
  v-model="social.url"
  placeholder="https://..."
  :validate="false"
/>
```

### Écouter l'état de validation

```vue
<template>
  <UrlInput
    v-model="form.site_url"
    @validation="handleValidation"
  />
  <p v-if="!isUrlValid" class="text-red-600">
    L'URL n'est pas valide
  </p>
</template>

<script setup lang="ts">
const form = ref({ site_url: '' });
const isUrlValid = ref(true);

function handleValidation(isValid: boolean) {
  isUrlValid.value = isValid;
}
</script>
```

## Comportement de validation

### Par défaut (validation au blur)

L'erreur s'affiche uniquement lorsque l'utilisateur quitte le champ (événement `blur`). C'est le comportement par défaut pour une meilleure UX.

```vue
<UrlInput v-model="form.site_url" />
```

### Validation en temps réel

Pour afficher l'erreur pendant la saisie, utilisez `show-error-on-input` :

```vue
<UrlInput
  v-model="form.site_url"
  :show-error-on-input="true"
/>
```

## Exemples de valeurs

| Valeur saisie | Valide (défaut) | Valide (allowHttp=false) | Message d'erreur |
|---------------|-----------------|--------------------------|------------------|
| `https://example.com` | ✅ Oui | ✅ Oui | - |
| `http://example.com` | ✅ Oui | ❌ Non | "URL doit utiliser HTTPS (https://)" |
| `https://example.com/path` | ✅ Oui | ✅ Oui | - |
| `https://sub.example.com` | ✅ Oui | ✅ Oui | - |
| `https://example.com:8080` | ✅ Oui | ✅ Oui | - |
| `example.com` | ❌ Non | ❌ Non | "URL invalide" |
| `not a url` | ❌ Non | ❌ Non | "URL invalide" |
| (vide avec required) | ❌ Non | ❌ Non | "Ce champ est requis" |
| (vide sans required) | ✅ Oui | ✅ Oui | - |

## Cas d'usage

### 1. Site web d'entreprise

```vue
<UrlInput
  v-model="company.site_url"
  label="Site web"
  :required="true"
/>
```

### 2. RIB en ligne

```vue
<UrlInput
  v-model="organisation.rib_url"
  label="RIB (URL)"
  placeholder="https://..."
/>
```

### 3. Réseaux sociaux (validation désactivée)

```vue
<UrlInput
  v-model="social.url"
  placeholder="https://..."
  :validate="false"
/>
```

### 4. API sécurisée (HTTPS uniquement)

```vue
<UrlInput
  v-model="webhook.url"
  label="URL du webhook"
  :allow-http="false"
  :required="true"
  hint="Doit utiliser HTTPS pour des raisons de sécurité"
/>
```

## Tests

Le composant est entièrement testé avec **23 tests** couvrant :
- Validation HTTP/HTTPS
- Option `allowHttp`
- URLs avec path, query, fragment, port, sous-domaine
- Props personnalisables
- Événements de validation
- Feedback visuel

Voir `tests/components/UrlInput.test.ts` pour tous les cas de test.

Pour lancer les tests :

```bash
npm test -- tests/components/UrlInput.test.ts
```

## Intégration dans les formulaires

Le composant est déjà intégré dans les formulaires suivants :

- `components/CompanyForm.vue` - Champ "Site web"
- `components/partnership/CompanyForm.vue` - Champ "Site web" et URLs de réseaux sociaux
- `components/OrganisationForm.vue` - Champ "RIB (URL)"

## Différences avec un input standard

| Fonctionnalité | `<input type="url">` | `<UrlInput>` |
|----------------|---------------------|--------------|
| Validation HTML5 | ✅ | ✅ |
| Messages personnalisés | ❌ | ✅ |
| Validation regex | ❌ | ✅ |
| Option HTTPS uniquement | ❌ | ✅ |
| Validation temps réel | ❌ | ✅ |
| Événements validation | ❌ | ✅ |
| Styling erreur | ❌ | ✅ |

## Voir aussi

- `components/SiretInput.vue` - Composant pour les numéros SIRET
- `components/EmailInput.vue` - Composant pour les emails (à créer)
- `constants/validation.ts` - Définitions des regex et messages de validation
