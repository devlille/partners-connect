# SiretInput Component

Composant de saisie dédié pour les numéros SIRET avec validation automatique du format.

## Caractéristiques

- ✅ **Validation automatique** : Vérifie que le SIRET contient exactement 14 chiffres
- 🧹 **Nettoyage des données** : Supprime automatiquement les caractères non-numériques
- 🎯 **Limitation de saisie** : Limite automatiquement à 14 caractères
- 🔴 **Messages d'erreur** : Affiche des messages d'erreur clairs et traduits
- 📱 **Mobile-friendly** : Utilise `inputmode="numeric"` pour afficher le clavier numérique
- ♿ **Accessible** : Labels et messages d'erreur associés correctement

## Validation

Le composant valide le format SIRET selon la regex définie dans `constants/validation.ts` :

```typescript
SIRET_REGEX = /^\d{14}$/
```

Le numéro SIRET doit contenir exactement **14 chiffres**.

## Messages d'erreur

Les messages d'erreur sont définis dans `constants/validation.ts` :

- **Format invalide** : "Numéro SIRET invalide (14 chiffres)"
- **Champ requis** : "Ce champ est requis" (si `required=true`)

## Utilisation de base

```vue
<template>
  <SiretInput v-model="company.siret" />
</template>

<script setup lang="ts">
const company = ref({
  siret: ''
});
</script>
```

## Props

| Prop               | Type             | Défaut     | Description                        |
| ------------------ | ---------------- | ---------- | ---------------------------------- |
| `modelValue`       | `string \| null` | -          | Valeur du champ (v-model)          |
| `label`            | `string`         | `'SIRET'`  | Texte du label                     |
| `disabled`         | `boolean`        | `false`    | Désactive le champ                 |
| `required`         | `boolean`        | `false`    | Marque le champ comme requis       |
| `hint`             | `string`         | -          | Texte d'aide sous le champ         |
| `inputClass`       | `string`         | `'w-full'` | Classes CSS additionnelles         |
| `validate`         | `boolean`        | `true`     | Active/désactive la validation     |
| `showErrorOnInput` | `boolean`        | `false`    | Affiche l'erreur pendant la saisie |

## Événements

| Événement           | Payload          | Description                            |
| ------------------- | ---------------- | -------------------------------------- |
| `update:modelValue` | `string \| null` | Émis quand la valeur change            |
| `validation`        | `boolean`        | Émis quand l'état de validation change |

## Exemples d'utilisation

### Champ requis

```vue
<SiretInput
  v-model="form.siret"
  :required="true"
/>
```

### Avec texte d'aide personnalisé

```vue
<SiretInput
  v-model="form.siret"
  hint="Le SIRET est composé du SIREN (9 chiffres) + NIC (5 chiffres)"
/>
```

### Avec label personnalisé

```vue
<SiretInput
  v-model="form.siret"
  label="Numéro SIRET de l'entreprise"
/>
```

### Avec validation en temps réel

```vue
<SiretInput
  v-model="form.siret"
  :show-error-on-input="true"
/>
```

### Désactivé

```vue
<SiretInput
  v-model="form.siret"
  :disabled="isLoading"
/>
```

### Écouter l'état de validation

```vue
<template>
  <SiretInput
    v-model="form.siret"
    @validation="handleValidation"
  />
  <p v-if="!isSiretValid" class="text-red-600">
    Le SIRET n'est pas valide
  </p>
</template>

<script setup lang="ts">
const form = ref({ siret: '' });
const isSiretValid = ref(true);

function handleValidation(isValid: boolean) {
  isSiretValid.value = isValid;
}
</script>
```

### Sans validation

```vue
<SiretInput
  v-model="form.siret"
  :validate="false"
/>
```

## Comportement de validation

### Par défaut (validation au blur)

L'erreur s'affiche uniquement lorsque l'utilisateur quitte le champ (événement `blur`). C'est le comportement par défaut pour une meilleure UX.

```vue
<SiretInput v-model="form.siret" />
```

### Validation en temps réel

Pour afficher l'erreur pendant la saisie, utilisez `show-error-on-input` :

```vue
<SiretInput
  v-model="form.siret"
  :show-error-on-input="true"
/>
```

## Exemples de valeurs

| Valeur saisie        | Valeur stockée   | Valide | Message d'erreur                      |
| -------------------- | ---------------- | ------ | ------------------------------------- |
| `12345678901234`     | `12345678901234` | ✅ Oui | -                                     |
| `123 456 789 01234`  | `12345678901234` | ✅ Oui | -                                     |
| `123-456-789-01234`  | `12345678901234` | ✅ Oui | -                                     |
| `123`                | `123`            | ❌ Non | "Numéro SIRET invalide (14 chiffres)" |
| `12345678901`        | `12345678901`    | ❌ Non | "Numéro SIRET invalide (14 chiffres)" |
| `ABC123`             | `123`            | ❌ Non | "Numéro SIRET invalide (14 chiffres)" |
| (vide avec required) | `null`           | ❌ Non | "Ce champ est requis"                 |
| (vide sans required) | `null`           | ✅ Oui | -                                     |

## Tests

Le composant est entièrement testé. Voir `tests/components/SiretInput.test.ts` pour tous les cas de test.

Pour lancer les tests :

```bash
npm test -- tests/components/SiretInput.test.ts
```

## Intégration dans les formulaires

Le composant est déjà intégré dans les formulaires suivants :

- `components/CompanyForm.vue`
- `components/partnership/CompanyForm.vue`
- `components/OrganisationForm.vue`

## Voir aussi

- `constants/validation.ts` - Définitions des regex et messages de validation
- `components/VatInput.vue` - Composant similaire pour les numéros de TVA (à créer)
