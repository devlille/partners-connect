<template>
  <form @submit.prevent="onSave" novalidate>
    <div class="grid grid-cols-1 gap-4 mb-6">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom du pack*</label>
        <UInput
          id="name"
          v-model="form.name"
          type="text"
          required
          placeholder="Ex: Pack Or, Pack Argent..."
          :class="{ 'border-red-500': validationErrors.name }"
          :aria-invalid="!!validationErrors.name"
          :aria-describedby="validationErrors.name ? 'name-error' : undefined"
        />
        <p v-if="validationErrors.name" id="name-error" class="mt-1 text-sm text-red-600" role="alert">{{ validationErrors.name }}</p>
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
          :class="{ 'border-red-500': validationErrors.base_price }"
          :aria-invalid="!!validationErrors.base_price"
          :aria-describedby="validationErrors.base_price ? 'price-error' : undefined"
        />
        <p v-if="validationErrors.base_price" id="price-error" class="mt-1 text-sm text-red-600" role="alert">{{ validationErrors.base_price }}</p>
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

     
    </div>

    <!-- Section des options -->
    <div v-if="options && options.length > 0" class="border-t border-gray-200 pt-6 mb-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Options de sponsoring</h3>
      <p v-if="validationErrors.options" class="mb-4 text-sm text-red-600" role="alert" aria-live="polite">{{ validationErrors.options }}</p>

      <!-- Options obligatoires -->
      <div class="mb-6">
        <fieldset>
          <legend class="block text-sm font-medium text-gray-700 mb-3">Options obligatoires</legend>
          <div class="space-y-2" role="group" aria-label="Options obligatoires">
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
                {{ getOptionName(option) }} ({{ formatOptionPrice(option) }})
              </label>
            </div>
          </div>
        </fieldset>
      </div>

      <!-- Options optionnelles -->
      <div>
        <fieldset>
          <legend class="block text-sm font-medium text-gray-700 mb-3">Options optionnelles</legend>
          <div class="space-y-2" role="group" aria-label="Options optionnelles">
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
                {{ getOptionName(option) }} ({{ formatOptionPrice(option) }})
              </label>
            </div>
          </div>
        </fieldset>
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
import { sponsoringPackSchema, packOptionsSchema } from "~/utils/validation/schemas";

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

const { t } = useI18n();
const { handleError } = useErrorHandler();

const form = ref<PackFormData>({
  name: props.data.name || "",
  price: props.data.price || 0,
  max_quantity: props.data.max_quantity || undefined,
  requiredOptions: props.initialRequiredOptions || [],
  optionalOptions: props.initialOptionalOptions || []
})

const validationErrors = ref<Record<string, string>>({});

const { getOptionName } = useOptionTranslation();

/**
 * Format option price with quantity for typed_number options
 */
function formatOptionPrice(option: SponsoringOption): string {
  // Pour typed_number, afficher le prix avec la quantité
  if (option.type === 'typed_number' && 'fixed_quantity' in option) {
    const price = option.price ? `${option.price}€` : '0€';
    return `${price} ×${option.fixed_quantity}`;
  }
  // Pour les autres types
  return option.price ? `${option.price}€` : '0€';
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
  // Effacer l'erreur de validation si elle existe
  if (validationErrors.value['options']) {
    delete validationErrors.value['options'];
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
  // Effacer l'erreur de validation si elle existe
  if (validationErrors.value['options']) {
    delete validationErrors.value['options'];
  }
}

function onSave() {
  validationErrors.value = {};

  const { requiredOptions, optionalOptions, ...packData } = form.value;

  // Valider les données du pack
  const packValidation = sponsoringPackSchema.safeParse({
    name: packData.name,
    base_price: packData.price,
    max_quantity: packData.max_quantity,
  });

  if (!packValidation.success) {
    packValidation.error.issues.forEach((err: any) => {
      const field = err.path[0] as string;
      validationErrors.value[field] = t(err.message);
    });
  }

  // Valider les options
  const optionsValidation = packOptionsSchema.safeParse({
    requiredOptions,
    optionalOptions
  });

  if (!optionsValidation.success) {
    optionsValidation.error.issues.forEach((err: any) => {
      validationErrors.value['options'] = err.message;
    });
  }

  // Si des erreurs existent, ne pas émettre
  if (Object.keys(validationErrors.value).length > 0) {
    console.error('Validation errors:', validationErrors.value);
    return;
  }

  emit('save', {
    pack: packData,
    requiredOptions,
    optionalOptions
  })
}
</script>
