# ZipCodeInput Component

Composant de saisie dédié pour les codes postaux français avec validation automatique du format.

## Caractéristiques

- ✅ **Validation automatique** : Vérifie que le code postal contient exactement 5 chiffres
- 🧹 **Nettoyage des données** : Supprime automatiquement les caractères non-numériques
- 🎯 **Limitation de saisie** : Limite automatiquement à 5 caractères
- 🔴 **Messages d'erreur** : Affiche des messages d'erreur clairs et traduits
- 📱 **Mobile-friendly** : Utilise `inputmode="numeric"` pour afficher le clavier numérique
- ♿ **Accessible** : Labels et messages d'erreur associés correctement

## Validation

Le composant valide le format du code postal selon la regex définie dans `constants/validation.ts` :

```typescript
ZIP_CODE_REGEX = /^\d{5}$/
```

Le code postal doit contenir exactement **5 chiffres** (format français).

## Messages d'erreur

Les messages d'erreur sont définis dans `constants/validation.ts` :

- **Format invalide** : "Code postal invalide (5 chiffres)"
- **Champ requis** : "Ce champ est requis" (si `required=true`)

## Utilisation de base

```vue
<template>
  <ZipCodeInput v-model="address.zip_code" />
</template>

<script setup lang="ts">
const address = ref({
  zip_code: ''
});
</script>
```

## Props

| Prop | Type | Défaut | Description |
|------|------|--------|-------------|
| `modelValue` | `string \| null \| undefined` | - | Valeur du champ (v-model) |
| `label` | `string` | `'Code postal'` | Texte du label |
| `placeholder` | `string` | `'00000'` | Texte du placeholder |
| `disabled` | `boolean` | `false` | Désactive le champ |
| `required` | `boolean` | `false` | Marque le champ comme requis |
| `hint` | `string` | - | Texte d'aide sous le champ |
| `inputClass` | `string` | `'w-full'` | Classes CSS additionnelles |
| `validate` | `boolean` | `true` | Active/désactive la validation |
| `showErrorOnInput` | `boolean` | `false` | Affiche l'erreur pendant la saisie |

## Événements

| Événement | Payload | Description |
|-----------|---------|-------------|
| `update:modelValue` | `string \| null` | Émis quand la valeur change |
| `validation` | `boolean` | Émis quand l'état de validation change |

## Exemples d'utilisation

### Champ requis

```vue
<ZipCodeInput
  v-model="form.zip_code"
  :required="true"
/>
```

### Avec texte d'aide personnalisé

```vue
<ZipCodeInput
  v-model="form.zip_code"
  hint="Code postal français (5 chiffres)"
/>
```

### Avec label et placeholder personnalisés

```vue
<ZipCodeInput
  v-model="form.zip_code"
  label="CP"
  placeholder="75001"
/>
```

### Avec validation en temps réel

```vue
<ZipCodeInput
  v-model="form.zip_code"
  :show-error-on-input="true"
/>
```

### Désactivé

```vue
<ZipCodeInput
  v-model="form.zip_code"
  :disabled="isLoading"
/>
```

### Sans validation

```vue
<ZipCodeInput
  v-model="form.zip_code"
  :validate="false"
/>
```

### Écouter l'état de validation

```vue
<template>
  <ZipCodeInput
    v-model="form.zip_code"
    @validation="handleValidation"
  />
  <p v-if="!isZipCodeValid" class="text-red-600">
    Le code postal n'est pas valide
  </p>
</template>

<script setup lang="ts">
const form = ref({ zip_code: '' });
const isZipCodeValid = ref(true);

function handleValidation(isValid: boolean) {
  isZipCodeValid.value = isValid;
}
</script>
```

## Comportement de validation

### Par défaut (validation au blur)

L'erreur s'affiche uniquement lorsque l'utilisateur quitte le champ (événement `blur`). C'est le comportement par défaut pour une meilleure UX.

```vue
<ZipCodeInput v-model="form.zip_code" />
```

### Validation en temps réel

Pour afficher l'erreur pendant la saisie, utilisez `show-error-on-input` :

```vue
<ZipCodeInput
  v-model="form.zip_code"
  :show-error-on-input="true"
/>
```

## Exemples de valeurs

| Valeur saisie | Valeur stockée | Valide | Message d'erreur |
|---------------|----------------|--------|------------------|
| `75001` | `75001` | ✅ Oui | - |
| `75 001` | `75001` | ✅ Oui | - |
| `75-001` | `75001` | ✅ Oui | - |
| `750` | `750` | ❌ Non | "Code postal invalide (5 chiffres)" |
| `7500` | `7500` | ❌ Non | "Code postal invalide (5 chiffres)" |
| `ABC123` | `123` | ❌ Non | "Code postal invalide (5 chiffres)" |
| `750012345` | `75001` | ✅ Oui | - (tronqué à 5 chiffres) |
| (vide avec required) | `null` | ❌ Non | "Ce champ est requis" |
| (vide sans required) | `null` | ✅ Oui | - |

## Codes postaux français valides

Le composant accepte tous les codes postaux français sur 5 chiffres, incluant :

### France métropolitaine
- `01000` à `95999` - Départements métropolitains
- `75001` à `75020` - Paris
- `69001` à `69009` - Lyon
- `13001` à `13016` - Marseille

### Départements et territoires d'outre-mer (DOM-TOM)
- `97100` à `97690` - Guadeloupe, Martinique, Guyane, Réunion, Mayotte
- `98000` à `98999` - Nouvelle-Calédonie, Polynésie française, etc.

### Collectivités d'outre-mer (COM)
- `98600` à `98890` - Wallis-et-Futuna, Saint-Pierre-et-Miquelon, etc.

## Tests

Le composant est entièrement testé avec **30 tests** couvrant :
- Validation du format (5 chiffres)
- Rejet des formats invalides (trop court, trop long)
- Nettoyage des caractères non-numériques
- Codes postaux réels (Paris, Lyon, Marseille, DOM-TOM)
- Gestion des champs requis
- Événements de validation
- Props personnalisables
- Feedback visuel
- Attributs HTML (inputmode, maxlength, type)

Voir `tests/components/ZipCodeInput.test.ts` pour tous les cas de test.

Pour lancer les tests :

```bash
npm test -- tests/components/ZipCodeInput.test.ts
```

## Intégration dans les formulaires

Le composant est déjà intégré dans les formulaires suivants :

- `components/CompanyForm.vue` - Adresse du siège social
- `components/partnership/CompanyForm.vue` - Adresse du siège social

## Cas d'usage

### 1. Adresse d'entreprise

```vue
<ZipCodeInput
  v-model="company.head_office.zip_code"
  :required="true"
/>
```

### 2. Adresse de livraison

```vue
<ZipCodeInput
  v-model="delivery.zip_code"
  label="Code postal de livraison"
  :required="true"
/>
```

### 3. Adresse de facturation

```vue
<ZipCodeInput
  v-model="billing.zip_code"
  label="Code postal de facturation"
/>
```

## Internationalisation

**Note** : Ce composant est actuellement optimisé pour les codes postaux **français uniquement** (5 chiffres).

Pour supporter d'autres formats internationaux, vous devrez :
1. Créer un composant plus générique (ex: `PostalCodeInput`)
2. Ajouter une prop `country` pour adapter la validation
3. Gérer différentes regex selon le pays :
   - France : 5 chiffres (`/^\d{5}$/`)
   - USA : 5 chiffres ou format ZIP+4 (`/^\d{5}(-\d{4})?$/`)
   - UK : Format alphanumérique (`/^[A-Z]{1,2}\d[A-Z\d]? ?\d[A-Z]{2}$/`)
   - Canada : Format alphanumérique (`/^[A-Z]\d[A-Z] ?\d[A-Z]\d$/`)

## Différences avec un input standard

| Fonctionnalité | `<input type="text">` | `<ZipCodeInput>` |
|----------------|----------------------|------------------|
| Validation format | ❌ | ✅ |
| Messages personnalisés | ❌ | ✅ |
| Nettoyage auto | ❌ | ✅ |
| Limitation 5 chiffres | ❌ | ✅ |
| Clavier numérique mobile | ❌ | ✅ |
| Événements validation | ❌ | ✅ |
| Styling erreur | ❌ | ✅ |

## Voir aussi

- `components/SiretInput.vue` - Composant pour les numéros SIRET
- `components/UrlInput.vue` - Composant pour les URLs
- `constants/validation.ts` - Définitions des regex et messages de validation
