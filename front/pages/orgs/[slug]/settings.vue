<template>
  <h2>{{ org.name }}</h2>

  <form @submit.prevent="submitForm">
    <p>
      <label for="name">Nom</label>
      <input
        v-model="form.name"
        type="text"
        id="name"
        name="name"
        autocomplete="name"
        disabled
      />
    </p>
    <p>
      <label for="head_office">Head Office</label>
      <input
        type="text"
        id="head_office"
        name="head_office"
        autocomplete="address-level1"
        v-model="form['head_office']"
      />
    </p>

    <p class="buttons-bar">
      <input type="submit" value="Valider" />
    </p>
  </form>
</template>

<script setup lang="ts">
import { ref } from "vue";

const sdk = useSdk();
const route = useRoute();

const org = await sdk.getOrg(route.params.slug);

const form = ref(org);

async function submitForm() {
  console.log(org, form.value);
  await sdk.updateOrg(route.params.slug, form.value);
}

definePageMeta({
  layout: "organisation",
});

useHead({
  title: "Organisation | DevLille",
  script: [
    {
      src: "/js/main-nav.js",
      type: "text/javascript",
      defer: true,
    },
    {
      src: "/js/autoScroll.js",
      type: "text/javascript",
      defer: true,
    },
    {
      src: "/js/tabs.js",
      type: "text/javascript",
      defer: true,
    },
  ],
});
</script>
