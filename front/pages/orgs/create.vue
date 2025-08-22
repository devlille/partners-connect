<template>
  <form @submit.prevent="submitForm">
    <p>
      <label for="name">Nom</label>
      <input
        v-model="form.name"
        type="text"
        id="name"
        name="name"
        autocomplete="organization"
      />
    </p>

    <p>
      <label for="head_office">Head Office</label>
      <input
        v-model="form.head_office"
        type="text"
        id="head_office"
        name="head_office"
        autocomplete="street-address"
      />
    </p>

    <p>
      <label for="siret">SIRET</label>
      <input
        v-model="form.siret"
        type="text"
        id="siret"
        name="siret"
        inputmode="numeric"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="siren">SIREN</label>
      <input
        v-model="form.siren"
        type="text"
        id="siren"
        name="siren"
        inputmode="numeric"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="tva">TVA</label>
      <input
        v-model="form.tva"
        type="text"
        id="tva"
        name="tva"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="d_and_b">D&amp;B</label>
      <input
        v-model="form.d_and_b"
        type="text"
        id="d_and_b"
        name="d_and_b"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="nace">NACE</label>
      <input
        v-model="form.nace"
        type="text"
        id="nace"
        name="nace"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="naf">NAF</label>
      <input
        v-model="form.naf"
        type="text"
        id="naf"
        name="naf"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="duns">DUNS</label>
      <input
        v-model="form.duns"
        type="text"
        id="duns"
        name="duns"
        inputmode="numeric"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="iban">IBAN</label>
      <input
        v-model="form.iban"
        type="text"
        id="iban"
        name="iban"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="bic">BIC</label>
      <input
        v-model="form.bic"
        type="text"
        id="bic"
        name="bic"
        autocomplete="off"
      />
    </p>

    <p>
      <label for="rib_url">RIB (URL)</label>
      <input
        v-model="form.rib_url"
        type="url"
        id="rib_url"
        name="rib_url"
        autocomplete="url"
        placeholder="https://…"
      />
    </p>

    <p>
      <label for="representative_user_email">Email du représentant</label>
      <input
        v-model="form.representative_user_email"
        type="email"
        id="representative_user_email"
        name="representative_user_email"
        autocomplete="email"
      />
    </p>

    <p>
      <label for="representative_role">Rôle du représentant</label>
      <input
        v-model="form.representative_role"
        type="text"
        id="representative_role"
        name="representative_role"
        autocomplete="organization-title"
      />
    </p>

    <p class="buttons-bar">
      <input type="submit" value="Valider" />
    </p>
  </form>
</template>

<script setup lang="ts">
import { ref } from "vue";

const form = ref<Organisation>({
  name: "",
  head_office: "",
  siret: "",
  siren: "",
  tva: "",
  d_and_b: "",
  nace: "",
  naf: "",
  duns: "",
  iban: "",
  bic: "",
  rib_url: "",
  representative_user_email: "",
  representative_role: "",
  created_at: new Date().toString(),
  creation_location: new Date().toString(),
  published_at: "Lille",
});

async function submitForm() {
  await postOrgs({
    ...form.value,
  });
}

definePageMeta({
  layout: "organisation",
});

useHead({
  title: "Organisation | DevLille",
  script: [
    { src: "/js/main-nav.js", type: "text/javascript", defer: true },
    { src: "/js/autoScroll.js", type: "text/javascript", defer: true },
    { src: "/js/tabs.js", type: "text/javascript", defer: true },
  ],
});
</script>
