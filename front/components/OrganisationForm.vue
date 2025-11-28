<template>
  <form @submit.prevent="onSave">
    <!-- Bloc principal -->
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
      <div>
        <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
        <UInput
          id="name"
          v-model="initialState.name"
          type="text"
          autocomplete="organization"
          placeholder="Nom de l'organisation"
          class="w-full"
        />
      </div>

      <div>
        <label for="head_office" class="block text-sm font-medium text-gray-700 mb-1">Siège social</label>
        <UInput
          id="head_office"
          v-model="initialState.head_office"
          type="text"
          autocomplete="street-address"
          placeholder="Adresse du siège social"
          class="w-full"
        />
      </div>

      <SiretInput
        v-model="initialState.siret"
      />

      <div>
        <label for="siren" class="block text-sm font-medium text-gray-700 mb-1">SIREN</label>
        <UInput
          id="siren"
          v-model="initialState.siren"
          type="text"
          autocomplete="off"
          placeholder="Numéro SIREN"
          class="w-full"
        />
      </div>

      <UrlInput
        v-model="initialState.rib_url"
        label="RIB (URL)"
        placeholder="https://..."
      />

      <div>
        <label for="representative_user_email" class="block text-sm font-medium text-gray-700 mb-1">Email du représentant</label>
        <UInput
          id="representative_user_email"
          v-model="initialState.representative_user_email"
          type="email"
          autocomplete="email"
          placeholder="email@example.com"
          class="w-full"
        />
      </div>

      <div>
        <label for="representative_role" class="block text-sm font-medium text-gray-700 mb-1">Rôle du représentant</label>
        <UInput
          id="representative_role"
          v-model="initialState.representative_role"
          type="text"
          autocomplete="organization-title"
          placeholder="Directeur, Responsable, etc."
          class="w-full"
        />
      </div>

      <div>
        <label for="created_at" class="block text-sm font-medium text-gray-700 mb-1">Date de création</label>
        <UInput
          id="created_at"
          v-model="initialState.created_at"
          type="date"
          class="w-full"
        />
      </div>

      <div>
        <label for="creation_location" class="block text-sm font-medium text-gray-700 mb-1">Lieu de création</label>
        <UInput
          id="creation_location"
          v-model="initialState.creation_location"
          type="text"
          placeholder="Ville de création"
          class="w-full"
        />
      </div>

      <div>
        <label for="published_at" class="block text-sm font-medium text-gray-700 mb-1">Date de publication des statuts</label>
        <UInput
          id="published_at"
          v-model="initialState.published_at"
          type="date"
          class="w-full"
        />
      </div>
    </div>

    <!-- Panneau déroulant - Informations complémentaires -->
    <UAccordion :items="[{ label: 'Informations complémentaires', slot: 'complementary' }]" class="mb-6">
      <template #complementary>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 p-4">
          <div>
            <label for="tva" class="block text-sm font-medium text-gray-700 mb-1">TVA intracommunautaire</label>
            <UInput
              id="tva"
              v-model="initialState.tva"
              type="text"
              autocomplete="off"
              placeholder="Numéro de TVA"
              class="w-full"
            />
          </div>

          <div>
            <label for="d_and_b" class="block text-sm font-medium text-gray-700 mb-1">D&B</label>
            <UInput
              id="d_and_b"
              v-model="initialState.d_and_b"
              type="text"
              autocomplete="off"
              placeholder="D&B"
              class="w-full"
            />
          </div>

          <div>
            <label for="nace" class="block text-sm font-medium text-gray-700 mb-1">NACE</label>
            <UInput
              id="nace"
              v-model="initialState.nace"
              type="text"
              autocomplete="off"
              placeholder="Code NACE"
              class="w-full"
            />
          </div>

          <div>
            <label for="naf" class="block text-sm font-medium text-gray-700 mb-1">NAF</label>
            <UInput
              id="naf"
              v-model="initialState.naf"
              type="text"
              autocomplete="off"
              placeholder="Code NAF"
              class="w-full"
            />
          </div>

          <div>
            <label for="duns" class="block text-sm font-medium text-gray-700 mb-1">DUNS</label>
            <UInput
              id="duns"
              v-model="initialState.duns"
              type="text"
              autocomplete="off"
              placeholder="Numéro DUNS"
              class="w-full"
            />
          </div>

          <div>
            <label for="iban" class="block text-sm font-medium text-gray-700 mb-1">IBAN</label>
            <UInput
              id="iban"
              v-model="initialState.iban"
              type="text"
              autocomplete="off"
              placeholder="IBAN"
              class="w-full"
            />
          </div>

          <div>
            <label for="bic" class="block text-sm font-medium text-gray-700 mb-1">BIC</label>
            <UInput
              id="bic"
              v-model="initialState.bic"
              type="text"
              autocomplete="off"
              placeholder="BIC"
              class="w-full"
            />
          </div>
        </div>
      </template>
    </UAccordion>

    <div class="flex justify-end gap-4 pt-4">
      <UButton type="submit" color="primary" size="lg">
        Valider
      </UButton>
    </div>
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
