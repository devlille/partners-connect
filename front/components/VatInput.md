# VatInput Component

Composant de saisie dédié pour les numéros de TVA intracommunautaire européens avec validation automatique du format.

## Caractéristiques

- ✅ **Validation automatique** : Vérifie le format européen (2 lettres + 8-12 caractères alphanumériques)
- 🧹 **Nettoyage des données** : Supprime automatiquement les caractères non-alphanumériques
- 🔠 **Conversion majuscules** : Convertit automatiquement en majuscules
- 🎯 **Limitation de saisie** : Limite automatiquement à 13 caractères
- 🔴 **Messages d'erreur** : Affiche des messages d'erreur clairs et traduits
- 🇪🇺 **Multi-pays** : Supporte tous les formats de TVA européens
- ♿ **Accessible** : Labels et messages d'erreur associés correctement

## Validation

Le composant valide le format de TVA européen selon la regex définie dans `constants/validation.ts` :

```typescript
VAT_REGEX = /^[A-Z]{2}[A-Z0-9]{8,12}$/
```

Le numéro de TVA doit :
- Commencer par **2 lettres majuscules** (code pays ISO 3166-1 alpha-2)
- Suivi de **8 à 12 caractères alphanumériques**

## Messages d'erreur

Les messages d'erreur sont définis dans `constants/validation.ts` :

- **Format invalide** : "Numéro de TVA invalide"
- **Champ requis** : "Ce champ est requis" (si `required=true`)

## Utilisation de base

```vue
<template>
  <VatInput v-model="company.vat" />
</template>

<script setup lang="ts">
const company = ref({
  vat: ''
});
</script>
```

## Props

| Prop | Type | Défaut | Description |
|------|------|--------|-------------|
| `modelValue` | `string \| null \| undefined` | - | Valeur du champ (v-model) |
| `label` | `string` | `'TVA intracommunautaire'` | Texte du label |
| `placeholder` | `string` | `'FR00000000000'` | Texte du placeholder |
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
<VatInput
  v-model="form.vat"
  :required="true"
/>
```

### Avec texte d'aide personnalisé

```vue
<VatInput
  v-model="form.vat"
  hint="Format: 2 lettres (pays) + chiffres"
/>
```

### Avec label et placeholder personnalisés

```vue
<VatInput
  v-model="form.vat"
  label="Numéro de TVA"
  placeholder="DE123456789"
/>
```

### Avec validation en temps réel

```vue
<VatInput
  v-model="form.vat"
  :show-error-on-input="true"
/>
```

### Désactivé

```vue
<VatInput
  v-model="form.vat"
  :disabled="isLoading"
/>
```

### Sans validation

```vue
<VatInput
  v-model="form.vat"
  :validate="false"
/>
```

### Écouter l'état de validation

```vue
<template>
  <VatInput
    v-model="form.vat"
    @validation="handleValidation"
  />
  <p v-if="!isVatValid" class="text-red-600">
    Le numéro de TVA n'est pas valide
  </p>
</template>

<script setup lang="ts">
const form = ref({ vat: '' });
const isVatValid = ref(true);

function handleValidation(isValid: boolean) {
  isVatValid.value = isValid;
}
</script>
```

## Comportement de validation

### Par défaut (validation au blur)

L'erreur s'affiche uniquement lorsque l'utilisateur quitte le champ (événement `blur`). C'est le comportement par défaut pour une meilleure UX.

```vue
<VatInput v-model="form.vat" />
```

### Validation en temps réel

Pour afficher l'erreur pendant la saisie, utilisez `show-error-on-input` :

```vue
<VatInput
  v-model="form.vat"
  :show-error-on-input="true"
/>
```

## Exemples de valeurs

| Valeur saisie | Valeur stockée | Valide | Message d'erreur |
|---------------|----------------|--------|------------------|
| `FR12345678901` | `FR12345678901` | ✅ Oui | - |
| `fr12345678901` | `FR12345678901` | ✅ Oui | - (converti) |
| `FR 12 345 678 901` | `FR12345678901` | ✅ Oui | - (nettoyé) |
| `DE123456789` | `DE123456789` | ✅ Oui | - |
| `FR123` | `FR123` | ❌ Non | "Numéro de TVA invalide" |
| `12345678901` | `12345678901` | ❌ Non | "Numéro de TVA invalide" |
| `FRXYZ` | `FRXYZ` | ❌ Non | "Numéro de TVA invalide" |
| (vide avec required) | `null` | ❌ Non | "Ce champ est requis" |
| (vide sans required) | `null` | ✅ Oui | - |

## Formats de TVA européens supportés

Le composant supporte tous les formats de numéros de TVA intracommunautaire de l'Union Européenne :

### Formats par pays

| Pays | Code | Format | Exemple | Longueur |
|------|------|--------|---------|----------|
| 🇫🇷 France | FR | 2 lettres + 11 chiffres | `FR12345678901` | 13 |
| 🇩🇪 Allemagne | DE | 2 lettres + 9 chiffres | `DE123456789` | 11 |
| 🇧🇪 Belgique | BE | 2 lettres + 10 chiffres | `BE0123456789` | 12 |
| 🇮🇹 Italie | IT | 2 lettres + 11 chiffres | `IT12345678901` | 13 |
| 🇪🇸 Espagne | ES | 2 lettres + 9-11 car. | `ES12345678901` | 11-13 |
| 🇳🇱 Pays-Bas | NL | 2 lettres + 9 chiffres + 3 car. | `NL123456789B01` | 14 |
| 🇵🇹 Portugal | PT | 2 lettres + 9 chiffres | `PT123456789` | 11 |
| 🇦🇹 Autriche | AT | 2 lettres + 'U' + 8 chiffres | `ATU12345678` | 11 |
| 🇸🇪 Suède | SE | 2 lettres + 12 chiffres | `SE123456789001` | 14 |
| 🇵🇱 Pologne | PL | 2 lettres + 10 chiffres | `PL1234567890` | 12 |
| 🇬🇷 Grèce | EL | 2 lettres + 9 chiffres | `EL123456789` | 11 |
| 🇩🇰 Danemark | DK | 2 lettres + 8 chiffres | `DK12345678` | 10 |
| 🇫🇮 Finlande | FI | 2 lettres + 8 chiffres | `FI12345678` | 10 |
| 🇮🇪 Irlande | IE | 2 lettres + 8-9 car. | `IE1234567A` | 10-11 |
| 🇱🇺 Luxembourg | LU | 2 lettres + 8 chiffres | `LU12345678` | 10 |

**Note** : Le composant accepte de 8 à 12 caractères alphanumériques après le code pays, ce qui couvre tous les formats européens (sauf NL qui fait 14 caractères au total et est tronqué à 13).

## Tests

Le composant est entièrement testé avec **32 tests** couvrant :
- Validation du format européen
- Numéros de TVA réels de 10 pays différents
- Conversion automatique en majuscules
- Nettoyage des caractères non-alphanumériques
- Limitation à 13 caractères
- Gestion des champs requis
- Événements de validation
- Props personnalisables
- Feedback visuel
- Attributs HTML

Voir `tests/components/VatInput.test.ts` pour tous les cas de test.

Pour lancer les tests :

```bash
npm test -- tests/components/VatInput.test.ts
```

## Intégration dans les formulaires

Le composant est déjà intégré dans les formulaires suivants :

- `components/CompanyForm.vue` - Informations entreprise
- `components/partnership/CompanyForm.vue` - Informations entreprise (partenariat)
- `components/OrganisationForm.vue` - Informations complémentaires (accordéon)

## Cas d'usage

### 1. Entreprise française

```vue
<VatInput
  v-model="company.vat"
  :required="true"
/>
```

### 2. Entreprise européenne

```vue
<VatInput
  v-model="company.vat"
  label="Numéro de TVA européen"
  hint="Format: code pays (2 lettres) + numéro"
/>
```

### 3. Facturation internationale

```vue
<VatInput
  v-model="invoice.vat_number"
  label="TVA du client"
  :required="true"
/>
```

## Vérification de la TVA

**Note importante** : Ce composant valide uniquement le **format** du numéro de TVA, pas son **existence réelle**.

Pour vérifier qu'un numéro de TVA existe vraiment, vous devez :
1. Utiliser l'API VIES (VAT Information Exchange System) de l'UE
2. Effectuer cette vérification côté serveur
3. Endpoint : `http://ec.europa.eu/taxation_customs/vies/services/checkVatService`

Exemple d'intégration :

```typescript
// Côté serveur
async function verifyVat(vatNumber: string) {
  const countryCode = vatNumber.substring(0, 2);
  const number = vatNumber.substring(2);

  // Appel à l'API VIES
  const response = await fetch(`https://ec.europa.eu/taxation_customs/vies/rest-api/check-vat-number`, {
    method: 'POST',
    body: JSON.stringify({
      countryCode,
      vatNumber: number
    })
  });

  return response.json();
}
```

## Différences avec un input standard

| Fonctionnalité | `<input type="text">` | `<VatInput>` |
|----------------|----------------------|--------------|
| Validation format | ❌ | ✅ |
| Messages personnalisés | ❌ | ✅ |
| Conversion majuscules | ❌ | ✅ |
| Nettoyage auto | ❌ | ✅ |
| Support multi-pays | ❌ | ✅ |
| Limitation longueur | ❌ | ✅ |
| Événements validation | ❌ | ✅ |
| Styling erreur | ❌ | ✅ |

## Voir aussi

- `components/SiretInput.vue` - Composant pour les numéros SIRET
- `components/UrlInput.vue` - Composant pour les URLs
- `components/ZipCodeInput.vue` - Composant pour les codes postaux
- `constants/validation.ts` - Définitions des regex et messages de validation
- [VIES VAT Validation](https://ec.europa.eu/taxation_customs/vies/) - Service de validation européen
