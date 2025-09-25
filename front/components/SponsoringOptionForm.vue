<template>
  <form @submit.prevent="onSave">
    <p>
      <label for="name">Nom de l'option*</label>
      <input
        id="name"
        v-model="form.name"
        type="text"
        name="name"
        required
        placeholder="Ex: Logo sur T-shirt, Stand premium..."
      >
    </p>

    <p>
      <label for="description">Description</label>
      <textarea
        id="description"
        v-model="form.description"
        name="description"
        rows="4"
        placeholder="Description détaillée de l'option de sponsoring"
      ></textarea>
    </p>

    <p>
      <label for="price">Prix (€)</label>
      <input
        id="price"
        v-model.number="form.price"
        type="number"
        name="price"
        min="0"
        step="0.01"
        placeholder="0.00 (laisser vide si gratuit)"
      >
    </p>

    <p class="buttons-bar">
      <input type="submit" value="Valider">
    </p>
  </form>
</template>

<script lang="ts" setup>
import type { CreateSponsoringOption } from "~/utils/api";

const { data } = defineProps<{ data: Partial<CreateSponsoringOption> }>()

const emit = defineEmits<{
  (e: 'save', payload: CreateSponsoringOption): void
}>()

const form = ref<CreateSponsoringOption>({
  name: data.name || "",
  description: data.description || "",
  price: data.price || undefined
})

function onSave() {
  emit('save', form.value)
}
</script>