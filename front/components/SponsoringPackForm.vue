<template>
  <form @submit.prevent="onSave">
    <div class="grid grid-cols-1 gap-4 mb-6">
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

    <!-- Section des options -->
    <div v-if="options && options.length > 0" class="border-t border-gray-200 pt-6 mb-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Options de sponsoring</h3>

      <!-- Options obligatoires -->
      <div class="mb-6">
        <label class="block text-sm font-medium text-gray-700 mb-3">Options obligatoires</label>
        <div class="space-y-2">
          <div v-for="option in options" :key="`required-${option.id}`" class="flex items-center space-x-3">
            <input
              :id="`required-${option.id}`"
              :checked="form.requiredOptions.includes(option.id)"
              type="checkbox"
              :value="option.id"
              class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              @change="handleRequiredOptionChange(option.id, ($event.target as HTMLInputElement).checked)"
            />
            <label :for="`required-${option.id}`" class="text-sm text-gray-700 cursor-pointer">
              {{ getOptionName(option) }} ({{ option.price || 0 }}€)
            </label>
          </div>
        </div>
      </div>

      <!-- Options optionnelles -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-3">Options optionnelles</label>
        <div class="space-y-2">
          <div v-for="option in options" :key="`optional-${option.id}`" class="flex items-center space-x-3">
            <input
              :id="`optional-${option.id}`"
              :checked="form.optionalOptions.includes(option.id)"
              type="checkbox"
              :value="option.id"
              class="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
              @change="handleOptionalOptionChange(option.id, ($event.target as HTMLInputElement).checked)"
            />
            <label :for="`optional-${option.id}`" class="text-sm text-gray-700 cursor-pointer">
              {{ getOptionName(option) }} ({{ option.price || 0 }}€)
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
import type { CreateSponsoringPack, SponsoringOption } from "~/utils/api";

interface PackFormData extends CreateSponsoringPack {
  requiredOptions: string[];
  optionalOptions: string[];
}

const props = defineProps<{
  data: Partial<CreateSponsoringPack>;
  options?: SponsoringOption[];
  initialRequiredOptions?: string[];
  initialOptionalOptions?: string[];
}>()

const emit = defineEmits<{
  (e: 'save', payload: { pack: CreateSponsoringPack; requiredOptions: string[]; optionalOptions: string[] }): void
}>()

const form = ref<PackFormData>({
  name: props.data.name || "",
  price: props.data.price || 0,
  with_booth: props.data.with_booth || false,
  nb_tickets: props.data.nb_tickets || 0,
  max_quantity: props.data.max_quantity || undefined,
  requiredOptions: props.initialRequiredOptions || [],
  optionalOptions: props.initialOptionalOptions || []
})

function getOptionName(option: SponsoringOption): string {
  // Récupérer la première traduction disponible
  if (option.translations) {
    const firstTranslation = Object.values(option.translations)[0];
    if (firstTranslation && typeof firstTranslation === 'object' && 'name' in firstTranslation) {
      return firstTranslation.name as string;
    }
  }
  return option.name || 'Option sans nom';
}

function handleRequiredOptionChange(optionId: string, checked: boolean) {
  if (checked) {
    // Ajouter aux options obligatoires
    if (!form.value.requiredOptions.includes(optionId)) {
      form.value.requiredOptions.push(optionId);
    }
    // Retirer des options optionnelles si elle y est
    form.value.optionalOptions = form.value.optionalOptions.filter(id => id !== optionId);
  } else {
    // Retirer des options obligatoires
    form.value.requiredOptions = form.value.requiredOptions.filter(id => id !== optionId);
  }
}

function handleOptionalOptionChange(optionId: string, checked: boolean) {
  if (checked) {
    // Ajouter aux options optionnelles
    if (!form.value.optionalOptions.includes(optionId)) {
      form.value.optionalOptions.push(optionId);
    }
    // Retirer des options obligatoires si elle y est
    form.value.requiredOptions = form.value.requiredOptions.filter(id => id !== optionId);
  } else {
    // Retirer des options optionnelles
    form.value.optionalOptions = form.value.optionalOptions.filter(id => id !== optionId);
  }
}

function onSave() {
  const { requiredOptions, optionalOptions, ...packData } = form.value;
  emit('save', {
    pack: packData,
    requiredOptions,
    optionalOptions
  })
}
</script>
