<template>
  <header>
    <MainTitle> Demande de Partenariat </MainTitle>
  </header>

  <div id="container">
    <main>
      <form>
        <TextInput
          id="ip-name"
          label="Nom"
          type="text"
          name="ip-name"
          autocomplete="name"
        />
        <SelectInput
          id="sel-pack"
          label="Pack de sponsoring"
          name="sel-pack"
        >
          <option v-for="pack in packs" :key="pack.id" :value="pack.id">
            {{ pack.name }} - {{ pack.base_price }} €
          </option>
        </SelectInput>
        <TextInput
          id="ip-email"
          label="Email"
          type="email"
          name="ip-email"
          autocomplete="email"
        />
        <PhoneInput
          id="ip-phone"
          label="Tél."
          name="ip-phone"
          autocomplete="tel"
        />

        <OptionsInput
          legend="Options de sponsoring"
          :options="options"
        />

        <p class="buttons-bar">
          <input type="submit" value="Valider" />
        </p>
      </form>
      
      <div class="auth-section">
        <p>Vous êtes un organisateur d'événements ?</p>
        <NuxtLink to="/login" class="login-link">Se connecter pour gérer vos organisations</NuxtLink>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { getOrgsOrgSlugEventsEventSlugPacks, getOrgsOrgSlugEventsEventSlugOptions, type SponsoringPack, type SponsoringOption } from "~/utils/api";

definePageMeta({
  layout: "minimal",
  auth: false,
});

const packs = ref<SponsoringPack[]>([]);
const options = ref<SponsoringOption[]>([ { id: 1, name: 'Option 1', price: 100 }, { id: 2, name: 'Option 2', price: 200 }]);

onMounted(async () => {
  const orgSlug = "test"
  const eventSlug = 'devlille';

  if (false && orgSlug && eventSlug) {
    try {
      const [packsResponse, optionsResponse] = await Promise.all([
        getOrgsOrgSlugEventsEventSlugPacks(orgSlug, eventSlug),
        getOrgsOrgSlugEventsEventSlugOptions(orgSlug, eventSlug)
      ]);
      packs.value = packsResponse.data;
      options.value = optionsResponse.data;
    } catch (error) {
      console.error('Failed to load sponsoring data:', error);
    }
  }
});

useHead({
  title: "Demande de Partenariat | DevLille",
  bodyAttrs: {
    id: "partners",
  }
});
</script>
