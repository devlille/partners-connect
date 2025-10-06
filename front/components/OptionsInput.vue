<template>
  <fieldset v-if="options.length > 0">
    <legend>{{ legend }}</legend>
    <div v-for="option in options" :key="option.id">
      <label>
        <input
          :id="`option-${option.id}`"
          type="checkbox"
          :name="`option-${option.id}`"
          :value="option.id"
        />
        {{ option.name }} - {{ option.price }} €
      </label>
    </div>
  </fieldset>
</template>

<script setup lang="ts">
import type { SponsoringOption } from "~/utils/api";

defineProps<{
  legend: string;
  options: SponsoringOption[];
}>();
</script>

<style lang="css" scoped>
  fieldset {
    border: 1px solid white;
  }
  label, legend {
    color: white;
  }
</style>
