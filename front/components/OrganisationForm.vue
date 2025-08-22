<template>
  <form @submit.prevent="onSave">
    <p>
      <label for="name">Nom</label>
      <input
        id="name"
        v-model="initialState.name"
        type="text"
        name="name"
        autocomplete="organization"
      >
    </p>

    <p>
      <label for="head_office">Head Office</label>
      <input
        id="head_office"
        v-model="initialState.head_office"
        type="text"
        name="head_office"
        autocomplete="street-address"
      >
    </p>

    <p>
      <label for="siret">SIRET</label>
      <input
        id="siret"
        v-model="initialState.siret"
        type="text"
        name="siret"
        inputmode="numeric"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="siren">SIREN</label>
      <input
        id="siren"
        v-model="initialState.siren"
        type="text"
        name="siren"
        inputmode="numeric"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="tva">TVA</label>
      <input
        id="tva"
        v-model="initialState.tva"
        type="text"
        name="tva"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="d_and_b">D&amp;B</label>
      <input
        id="d_and_b"
        v-model="initialState.d_and_b"
        type="text"
        name="d_and_b"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="nace">NACE</label>
      <input
        id="nace"
        v-model="initialState.nace"
        type="text"
        name="nace"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="naf">NAF</label>
      <input
        id="naf"
        v-model="initialState.naf"
        type="text"
        name="naf"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="duns">DUNS</label>
      <input
        id="duns"
        v-model="initialState.duns"
        type="text"
        name="duns"
        inputmode="numeric"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="iban">IBAN</label>
      <input
        id="iban"
        v-model="initialState.iban"
        type="text"
        name="iban"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="bic">BIC</label>
      <input
        id="bic"
        v-model="initialState.bic"
        type="text"
        name="bic"
        autocomplete="off"
      >
    </p>

    <p>
      <label for="rib_url">RIB (URL)</label>
      <input
        id="rib_url"
        v-model="initialState.rib_url"
        type="url"
        name="rib_url"
        autocomplete="url"
        placeholder="https://…"
      >
    </p>

    <p>
      <label for="representative_user_email">Email du représentant</label>
      <input
        id="representative_user_email"
        v-model="initialState.representative_user_email"
        type="email"
        name="representative_user_email"
        autocomplete="email"
      >
    </p>

    <p>
      <label for="representative_role">Rôle du représentant</label>
      <input
        id="representative_role"
        v-model="initialState.representative_role"
        type="text"
        name="representative_role"
        autocomplete="organization-title"
      >
    </p>

    <p>
      <label for="created_at">Date de Création</label>
      <input
        id="created_at"
        v-model="initialState.created_at"
        type="date"
        name="created_at"
      >
    </p>

    <p>
      <label for="representative_role">Lieu de Création</label>
      <input
        id="creation_location"
        v-model="initialState.creation_location"
        type="text"
        name="creation_location"
      >
    </p>
    <p>
      <label for="published_at">Lieu de publication des status</label>
      <input
        id="published_at"
        v-model="initialState.published_at"
        type="date"
        name="published_at"
      >
    </p>

    <p class="buttons-bar">
      <input type="submit" value="Valider" >
    </p>
  </form>
</template>

<script lang="ts" setup>
const { data } = defineProps<{ data: Organisation}>()

const emit = defineEmits<{
  (e: 'save', payload: Organisation): void
}>()
const initialState = ref<Organisation>(data)

function onSave() {
  emit('save', initialState.value)
}
</script>

