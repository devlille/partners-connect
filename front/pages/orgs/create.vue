<template>
  <organisation-form :data="data" @save="handleSave"/>
</template>

<script setup lang="ts">
import { ref } from "vue";
import authMiddleware from "~/middleware/auth";
const router = useRouter()

const data = ref<Organisation>({
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
  created_at: null,
  published_at: null,
  creation_location: "Lille",
});

async function handleSave(payload: Organisation) {
  // Filter out empty properties
  const filteredPayload = Object.fromEntries(
    Object.entries(payload).filter(([_, value]) => 
      value !== "" && value !== null && value !== undefined
    )
  ) as Organisation;
  
  await postOrgs(filteredPayload).then(response => router.push(`/orgs/${response.data["slug"]}/settings`))
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
