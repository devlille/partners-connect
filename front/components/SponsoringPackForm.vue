<template>
  <form @submit.prevent="onSave">
    <p>
      <label for="name">Nom du pack*</label>
      <input
        id="name"
        v-model="form.name"
        type="text"
        name="name"
        required
        placeholder="Ex: Pack Or, Pack Argent..."
      >
    </p>

    <p>
      <label for="price">Prix (€)*</label>
      <input
        id="price"
        v-model.number="form.price"
        type="number"
        name="price"
        required
        min="0"
        step="0.01"
        placeholder="0.00"
      >
    </p>

    <p>
      <label for="with_booth">Inclut un stand*</label>
      <input
        id="with_booth"
        v-model="form.with_booth"
        type="checkbox"
        name="with_booth"
      >
    </p>

    <p>
      <label for="nb_tickets">Nombre de billets*</label>
      <input
        id="nb_tickets"
        v-model.number="form.nb_tickets"
        type="number"
        name="nb_tickets"
        required
        min="0"
        placeholder="Nombre de billets inclus"
      >
    </p>

    <p>
      <label for="max_quantity">Quantité maximale</label>
      <input
        id="max_quantity"
        v-model.number="form.max_quantity"
        type="number"
        name="max_quantity"
        min="1"
        placeholder="Nombre maximum de sponsors pour ce pack"
      >
    </p>

    <p class="buttons-bar">
      <input type="submit" value="Valider">
    </p>
  </form>
</template>

<script lang="ts" setup>
import type { SponsoringPack, CreateSponsoringPack } from "~/utils/api";

const { data } = defineProps<{ data: Partial<CreateSponsoringPack> }>()

const emit = defineEmits<{
  (e: 'save', payload: CreateSponsoringPack): void
}>()

const form = ref<CreateSponsoringPack>({
  name: data.name || "",
  price: data.price || 0,
  with_booth: data.with_booth || false,
  nb_tickets: data.nb_tickets || 0,
  max_quantity: data.max_quantity || undefined
})

function onSave() {
  emit('save', form.value)
}
</script>