<template>
  <UButton
    size="sm"
    variant="ghost"
    color="neutral"
    icon="i-heroicons-paint-brush"
    :loading="generating"
    @click="handleClick"
  >
    {{ $t("flyer.generate.button") }}
  </UButton>
</template>

<script setup lang="ts">
import { postOrgsEventsPartnershipsFlyer } from "~/utils/api";
import { useSponsorsStore } from "~/stores/sponsors";

interface Props {
  orgSlug: string;
  eventSlug: string;
  partnershipId: string;
}

const props = defineProps<Props>();

const { t } = useI18n();
const toast = useToast();
const sponsorsStore = useSponsorsStore();

const generating = ref(false);

async function handleClick() {
  if (generating.value) return;
  generating.value = true;
  try {
    const response = await postOrgsEventsPartnershipsFlyer(
      props.orgSlug,
      props.eventSlug,
      props.partnershipId,
    );
    const url = response.data?.url;
    if (url) {
      sponsorsStore.updateSponsor(props.partnershipId, {
        communication_support_url: url,
      });
    }
    toast.add({ title: t("flyer.generate.success"), color: "success" });
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } };
    toast.add({
      title: t("flyer.generate.failure"),
      description: err.response?.data?.message ?? t("flyer.errors.generic"),
      color: "error",
    });
  } finally {
    generating.value = false;
  }
}
</script>
