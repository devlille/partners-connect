<template>
  <fieldset v-if="options.length > 0" class="options-fieldset">
    <legend>{{ legend }}</legend>

    <div v-for="option in options" :key="option.id" class="option-item">
      <!-- Text Option - Simple checkbox -->
      <div v-if="option.type === 'text'" class="option-text">
        <label>
          <input
            :id="`option-${option.id}`"
            type="checkbox"
            :checked="isOptionSelected(option.id)"
            @change="handleTextChange(option.id, ($event.target as HTMLInputElement).checked)"
          />
          {{ option.name }}
          <span v-if="option.price" class="option-price">- {{ option.price }} €</span>
        </label>
        <p v-if="option.description" class="option-description">{{ option.description }}</p>
      </div>

      <!-- Typed Quantitative Option - Checkbox + Number input -->
      <div v-if="option.type === 'typed_quantitative'" class="option-quantitative">
        <label>
          <input
            :id="`option-${option.id}`"
            type="checkbox"
            :checked="isOptionSelected(option.id)"
            @change="handleQuantitativeCheckboxChange(option.id, ($event.target as HTMLInputElement).checked)"
          />
          {{ option.name }}
          <span v-if="option.price" class="option-price">- {{ option.price }} € / unité</span>
        </label>
        <p v-if="option.description" class="option-description">{{ option.description }}</p>

        <div v-if="isOptionSelected(option.id)" class="quantity-input">
          <label :for="`quantity-${option.id}`" class="quantity-label"> Quantité : </label>
          <input
            :id="`quantity-${option.id}`"
            type="number"
            min="1"
            :value="getOptionQuantity(option.id)"
            @input="handleQuantityChange(option.id, ($event.target as HTMLInputElement).value)"
            class="quantity-field"
          />
        </div>
      </div>

      <!-- Typed Number Option - Display only (fixed quantity) -->
      <div v-if="option.type === 'typed_number'" class="option-number">
        <label>
          <input
            :id="`option-${option.id}`"
            type="checkbox"
            :checked="isOptionSelected(option.id)"
            @change="handleNumberChange(option.id, ($event.target as HTMLInputElement).checked, option.fixed_quantity)"
          />
          {{ option.name }}
          <span class="fixed-quantity">({{ option.fixed_quantity }} unités)</span>
          <span v-if="option.price" class="option-price">- {{ option.price }} €</span>
        </label>
        <p v-if="option.description" class="option-description">{{ option.description }}</p>
      </div>

      <!-- Typed Selectable Option - Checkbox + Select dropdown -->
      <div v-if="option.type === 'typed_selectable'" class="option-selectable">
        <label>
          <input
            :id="`option-${option.id}`"
            type="checkbox"
            :checked="isOptionSelected(option.id)"
            @change="handleSelectableCheckboxChange(option.id, ($event.target as HTMLInputElement).checked)"
          />
          {{ option.name }}
          <span v-if="option.price" class="option-price">- {{ option.price }} €</span>
        </label>
        <p v-if="option.description" class="option-description">{{ option.description }}</p>

        <div v-if="isOptionSelected(option.id)" class="selectable-input">
          <label :for="`select-${option.id}`" class="select-label"> Choisissez : </label>
          <select
            :id="`select-${option.id}`"
            :value="getOptionSelectedValue(option.id)"
            @change="handleSelectChange(option.id, ($event.target as HTMLSelectElement).value)"
            class="select-field"
          >
            <option value="">-- Sélectionnez une option --</option>
            <option
              v-for="(value, index) in option.selectable_values"
              :key="index"
              :value="getSelectableValueId(value)"
            >
              {{ formatSelectableValue(value, option.price) }}
            </option>
          </select>
        </div>
      </div>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import type { SponsoringOptionSchema, PartnershipOptionSelection, PartnershipOptionSelectionType } from "~/utils/api";

const props = defineProps<{
  legend: string;
  options: SponsoringOptionSchema[];
  modelValue: PartnershipOptionSelection[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: PartnershipOptionSelection[]): void;
}>();

// Check if an option is selected
function isOptionSelected(optionId: string): boolean {
  return props.modelValue.some(selection => selection.option_id === optionId);
}

// Get the quantity for a quantitative option
function getOptionQuantity(optionId: string): number {
  const selection = props.modelValue.find(s => s.option_id === optionId);
  return selection?.selected_quantity || 1;
}

// Get the selected value for a selectable option
function getOptionSelectedValue(optionId: string): string {
  const selection = props.modelValue.find(s => s.option_id === optionId);
  return selection?.selected_value_id || '';
}

// Handle text option change
function handleTextChange(optionId: string, checked: boolean) {
  if (checked) {
    emit('update:modelValue', [
      ...props.modelValue,
      {
        type: 'text_selection' as PartnershipOptionSelectionType,
        option_id: optionId
      }
    ]);
  } else {
    emit('update:modelValue', props.modelValue.filter(s => s.option_id !== optionId));
  }
}

// Handle quantitative option checkbox change
function handleQuantitativeCheckboxChange(optionId: string, checked: boolean) {
  if (checked) {
    emit('update:modelValue', [
      ...props.modelValue,
      {
        type: 'quantitative_selection' as PartnershipOptionSelectionType,
        option_id: optionId,
        selected_quantity: 1
      }
    ]);
  } else {
    emit('update:modelValue', props.modelValue.filter(s => s.option_id !== optionId));
  }
}

// Handle quantitative option quantity change
function handleQuantityChange(optionId: string, value: string) {
  const quantity = parseInt(value, 10);
  if (isNaN(quantity) || quantity < 1) return;

  const newSelections = props.modelValue.map(selection => {
    if (selection.option_id === optionId) {
      return {
        ...selection,
        selected_quantity: quantity
      };
    }
    return selection;
  });
  emit('update:modelValue', newSelections);
}

// Handle number option change (fixed quantity)
function handleNumberChange(optionId: string, checked: boolean, fixedQuantity: number) {
  if (checked) {
    emit('update:modelValue', [
      ...props.modelValue,
      {
        type: 'number_selection' as PartnershipOptionSelectionType,
        option_id: optionId,
        selected_quantity: fixedQuantity
      }
    ]);
  } else {
    emit('update:modelValue', props.modelValue.filter(s => s.option_id !== optionId));
  }
}

// Handle selectable option checkbox change
function handleSelectableCheckboxChange(optionId: string, checked: boolean) {
  if (checked) {
    emit('update:modelValue', [
      ...props.modelValue,
      {
        type: 'selectable_selection' as PartnershipOptionSelectionType,
        option_id: optionId,
        selected_value_id: ''
      }
    ]);
  } else {
    emit('update:modelValue', props.modelValue.filter(s => s.option_id !== optionId));
  }
}

// Handle selectable option value change
function handleSelectChange(optionId: string, value: string) {
  const newSelections = props.modelValue.map(selection => {
    if (selection.option_id === optionId) {
      return {
        ...selection,
        selected_value_id: value
      };
    }
    return selection;
  });
  emit('update:modelValue', newSelections);
}

// Get the ID/value from a selectable value (handles both string and object)
function getSelectableValueId(value: any): string {
  if (typeof value === 'string') {
    return value;
  }
  // If it's an object, try to get id or value property
  return value?.id || value?.value || JSON.stringify(value);
}

// Format selectable value for display
function formatSelectableValue(value: any, price?: number): string {
  let label = '';

  if (typeof value === 'string') {
    label = value;
  } else if (typeof value === 'object' && value !== null) {
    // If it's an object, try to extract name/label and price
    const name = value.name || value.label || value.value || JSON.stringify(value);
    const itemPrice = value.price || price;

    if (itemPrice) {
      return `${name} (${itemPrice} €)`;
    }
    return name;
  } else {
    label = String(value);
  }

  // Add price if available
  if (price) {
    return `${label} (${price} €)`;
  }

  return label;
}
</script>

<style lang="css" scoped>
.options-fieldset {
  border: 1px solid white;
  padding: 1.5rem;
  margin: 0 0 1.5rem 0;
  box-sizing: border-box;
}

legend {
  color: white;
  font-weight: 600;
  padding: 0 0.5rem;
}

.option-item {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.25rem;
}

.option-item:last-child {
  margin-bottom: 0;
}

label {
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.option-price {
  color: #fbbf24;
  font-weight: 600;
  margin-left: 0.5rem;
}

.fixed-quantity {
  color: #94a3b8;
  font-size: 0.875rem;
  margin-left: 0.5rem;
}

.option-description {
  color: #cbd5e1;
  font-size: 0.875rem;
  margin: 0.5rem 0 0 1.75rem;
}

.quantity-input,
.selectable-input {
  margin-top: 0.75rem;
  margin-left: 1.75rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.quantity-label,
.select-label {
  color: white;
  font-size: 0.875rem;
  min-width: 80px;
}

.quantity-field {
  width: 100px;
  padding: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 0.25rem;
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.select-field {
  flex: 1;
  max-width: 300px;
  padding: 0.5rem;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 0.25rem;
  background-color: rgba(255, 255, 255, 0.1);
  color: white;
}

.quantity-field:focus,
.select-field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

input[type="checkbox"] {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}
</style>
