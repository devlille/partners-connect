<template>
  <h2>{{ org.name }}</h2>

  <organisation-form :data="form" @save="onSave"/>
</template>

<script setup lang="ts">
import { ref } from "vue";
import authMiddleware from "~/middleware/auth";

const route = useRoute();
const org = await getOrgs(route.params.slug as string).then((r) => r.data);

const form = ref<Organisation>({
  name: org?.name ?? "",
  head_office: org?.head_office ?? "",
  siret: org?.siret ?? "",
  siren: org?.siren ?? "",
  tva: org?.tva ?? "",
  d_and_b: org?.d_and_b ?? org?.d_and_b ?? "",
  nace: org?.nace ?? "",
  naf: org?.naf ?? "",
  duns: org?.duns ?? "",
  iban: org?.iban ?? "",
  bic: org?.bic ?? "",
  rib_url: org?.rib_url ?? "",
  representative_user_email: org?.representative_user_email ?? "",
  representative_role: org?.representative_role ?? "",
  created_at: org.created_at,
  creation_location: org.creation_location,
  published_at: org.published_at,
});

async function onSave(organisation: Organisation) {
  await putOrgsOrgSlug(route.params.slug as string, organisation);
}

definePageMeta({
  layout: "organisation",
  middleware: authMiddleware,
  ssr: false
});

useHead({
  title: "Organisation | DevLille"
});
</script>
