<template>
  <div class="card">
    <h3>
      <NuxtLink :to="{ name: 'sponsor-id', params: { id: partner.id } }">{{
        partner.name
      }}</NuxtLink>
    </h3>
    <dl>
      <dt>Facturation</dt>
      <dd>{{ partner.billingDate }}</dd>
      <dt>Status</dt>
      <dd>{{ partner.status }}</dd>
      <dt>Pack</dt>
      <dd>{{ partner.pack }}</dd>
    </dl>
  </div>
</template>

<script setup lang="ts">
import type { Partner } from "~/types/partner";

const { partner } = defineProps<{ partner: Partner }>();
</script>
