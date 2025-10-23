<template>
  <form @submit.prevent="onSave">
    <div class="grid grid-cols-1 gap-4 mb-6">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom de l'option*</label>
        <UInput
          id="name"
          v-model="form.name"
          type="text"
          required
          placeholder="Ex: Logo sur T-shirt, Stand premium..."
        />
      </div>

      <div>
        <label for="description" class="block text-sm font-medium text-gray-700 mb-1">Description</label>
        <UTextarea
          id="description"
          v-model="form.description"
          :rows="4"
          placeholder="Description détaillée de l'option de sponsoring"
        />
      </div>

      <div>
        <label for="price" class="block text-sm font-medium text-gray-700 mb-1">Prix (€)</label>
        <UInput
          id="price"
          v-model.number="form.price"
          type="number"
          :min="0"
          :step="0.01"
          placeholder="0.00 (laisser vide si gratuit)"
        />
      </div>

      <!-- Section des packs -->
      <div v-if="packs && packs.length > 0" class="border-t border-gray-200 pt-4">
        <label class="block text-sm font-medium text-gray-700 mb-3">Associer à des packs</label>
        <div class="space-y-2">
          <div v-for="pack in packs" :key="pack.id" class="flex items-center space-x-3">
            <UCheckbox
              :id="`pack-${pack.id}`"
              v-model="form.selectedPacks"
              :value="pack.id"
            />
            <label :for="`pack-${pack.id}`" class="text-sm text-gray-700 cursor-pointer">
              {{ pack.name }} ({{ pack.base_price }}€)
            </label>
          </div>
        </div>
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
import type { CreateSponsoringOption, SponsoringPack } from "~/utils/api";

// Type pour les données du formulaire (simplifié)
type OptionFormData = {
  name: string;
  description: string;
  price: number | undefined;
  selectedPacks: string[];
};

// Type pour les données émises
type OptionFormEmit = {
  option: CreateSponsoringOption;
  selectedPacks: string[];
};

const props = defineProps<{
  data: Partial<OptionFormData>;
  packs?: SponsoringPack[];
}>()

const emit = defineEmits<{
  (e: 'save', payload: OptionFormEmit): void
}>()

const form = ref<OptionFormData>({
  name: props.data.name || "",
  description: props.data.description || "",
  price: props.data.price || undefined,
  selectedPacks: props.data.selectedPacks || []
})

function onSave() {
  const formattedData: CreateSponsoringOption = {
    translations: [
      {
        language: "fr",
        name: form.value.name,
        description: form.value.description || null
      }
    ],
    price: form.value.price || null
  };

  emit('save', {
    option: formattedData,
    selectedPacks: form.value.selectedPacks
  })
}
</script>