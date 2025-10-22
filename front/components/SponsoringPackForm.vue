<template>
  <form @submit.prevent="onSave">
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom du pack*</label>
        <UInput
          id="name"
          v-model="form.name"
          type="text"
          required
          placeholder="Ex: Pack Or, Pack Argent..."
        />
      </div>

      <div>
        <label for="price" class="block text-sm font-medium text-gray-700 mb-1">Prix (€)*</label>
        <UInput
          id="price"
          v-model.number="form.price"
          type="number"
          required
          :min="0"
          :step="0.01"
          placeholder="0.00"
        />
      </div>

      <div>
        <label for="nb_tickets" class="block text-sm font-medium text-gray-700 mb-1">Nombre de billets*</label>
        <UInput
          id="nb_tickets"
          v-model.number="form.nb_tickets"
          type="number"
          required
          :min="0"
          placeholder="Nombre de billets inclus"
        />
      </div>

      <div>
        <label for="max_quantity" class="block text-sm font-medium text-gray-700 mb-1">Quantité maximale</label>
        <UInput
          id="max_quantity"
          v-model.number="form.max_quantity"
          type="number"
          :min="1"
          placeholder="Nombre maximum de sponsors pour ce pack"
        />
      </div>

      <div class="flex items-center">
        <UCheckbox
          id="with_booth"
          v-model="form.with_booth"
          label="Inclut un stand"
        />
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
import type { CreateSponsoringPack } from "~/utils/api";

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