# SponsoringOptionsInput Component

## Description

Composant Vue pour gérer les différents types d'options de sponsoring lors de la création d'un partenariat.

## Types d'options supportés

### 1. Text (`text`)

Option simple avec checkbox uniquement.

**Exemple d'utilisation :**

- Logo sur le site web
- Mention dans la newsletter

**Propriétés :**

- `name`: Nom de l'option
- `description`: Description optionnelle
- `price`: Prix optionnel

**Rendu :**

```
☐ Logo sur le site web - 500 €
```

### 2. Typed Quantitative (`typed_quantitative`)

Option avec quantité variable définie par l'utilisateur.

**Exemple d'utilisation :**

- Offres d'emploi (nombre variable)
- Goodies personnalisés

**Propriétés :**

- `name`: Nom de l'option
- `description`: Description optionnelle
- `price`: Prix par unité
- `type_descriptor`: Type de quantité (ex: `job_offer`)

**Rendu :**

```
☐ Offres d'emploi - 100 € / unité
  Quantité : [___3___]
```

**Données envoyées :**

```typescript
{
  type: 'quantitative_selection',
  option_id: 'uuid',
  selected_quantity: 3
}
```

### 3. Typed Number (`typed_number`)

Option avec quantité fixe (non modifiable par l'utilisateur).

**Exemple d'utilisation :**

- Pack de 10 tickets
- 5 badges VIP

**Propriétés :**

- `name`: Nom de l'option
- `description`: Description optionnelle
- `price`: Prix total
- `type_descriptor`: Type de nombre (ex: `nb_ticket`)
- `fixed_quantity`: Quantité fixe

**Rendu :**

```
☐ Pack de tickets (10 unités) - 200 €
```

**Données envoyées :**

```typescript
{
  type: 'number_selection',
  option_id: 'uuid',
  selected_quantity: 10  // Valeur fixe
}
```

### 4. Typed Selectable (`typed_selectable`)

Option avec choix parmi des valeurs prédéfinies.

**Exemple d'utilisation :**

- Emplacement de stand (A1, A2, B1, B2)
- Taille de logo (Small, Medium, Large)

**Propriétés :**

- `name`: Nom de l'option
- `description`: Description optionnelle
- `price`: Prix
- `type_descriptor`: Type de sélection (ex: `booth`)
- `selectable_values`: Liste des valeurs possibles

**Rendu :**

```
☐ Emplacement de stand - 1000 €
  Choisissez : [-- Sélectionnez une option --▼]
                [Stand A1                      ]
                [Stand A2                      ]
                [Stand B1                      ]
```

**Données envoyées :**

```typescript
{
  type: 'selectable_selection',
  option_id: 'uuid',
  selected_value_id: 'Stand A1'
}
```

## Utilisation

```vue
<template>
  <SponsoringOptionsInput
    v-model="optionSelections"
    legend="Options de sponsoring"
    :options="availableOptions"
  />
</template>

<script setup>
import type { PartnershipOptionSelection, SponsoringOptionSchema } from '~/utils/api';

const optionSelections = ref<PartnershipOptionSelection[]>([]);
const availableOptions = ref<SponsoringOptionSchema[]>([]);
</script>
```

## Props

| Prop         | Type                           | Required | Description                    |
| ------------ | ------------------------------ | -------- | ------------------------------ |
| `legend`     | `string`                       | Oui      | Titre de la section d'options  |
| `options`    | `SponsoringOptionSchema[]`     | Oui      | Liste des options disponibles  |
| `modelValue` | `PartnershipOptionSelection[]` | Oui      | Sélections actuelles (v-model) |

## Events

| Event               | Payload                        | Description                          |
| ------------------- | ------------------------------ | ------------------------------------ |
| `update:modelValue` | `PartnershipOptionSelection[]` | Émis lorsque les sélections changent |

## Format des données

Les données sont envoyées au backend dans le format `PartnershipOptionSelection[]` :

```typescript
interface PartnershipOptionSelection {
  type: 'text_selection' | 'quantitative_selection' | 'number_selection' | 'selectable_selection';
  option_id: string;
  selected_quantity?: number;      // Pour quantitative et number
  selected_value_id?: string;      // Pour selectable
}
```

## Exemple complet

```typescript
// Données envoyées lors de la création d'un partenariat
const partnershipData = {
  company_id: 'company-uuid',
  pack_id: 'pack-uuid',
  option_selections: [
    {
      type: 'text_selection',
      option_id: 'logo-option-uuid'
    },
    {
      type: 'quantitative_selection',
      option_id: 'job-offer-uuid',
      selected_quantity: 3
    },
    {
      type: 'number_selection',
      option_id: 'tickets-uuid',
      selected_quantity: 10  // Valeur fixe de l'option
    },
    {
      type: 'selectable_selection',
      option_id: 'booth-uuid',
      selected_value_id: 'Stand A1'
    }
  ],
  // ... autres champs
};
```

## Validation

Le composant effectue une validation de base :

- Pour les quantités : minimum 1
- Pour les sélectables : au moins une valeur doit être choisie si l'option est cochée

## Styles

Le composant utilise des styles CSS scopés avec un thème sombre adapté au design du formulaire de partenariat.
