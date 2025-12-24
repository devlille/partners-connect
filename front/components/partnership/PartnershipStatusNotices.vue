<template>
  <div>
    <!-- Information message for validated but unpaid partnerships -->
    <NoticeBlock
      v-if="shouldShowCompletionMessage"
      :title="$t('partnershipNotices.completeInfo.title')"
      icon="i-heroicons-information-circle"
      variant="info"
    >
      <p class="mb-3">
        {{ $t('partnershipNotices.completeInfo.description') }}
      </p>
      <ul class="space-y-1 ml-5 list-disc">
        <li>
          <NuxtLink :to="`/${eventSlug}/${partnershipId}`" class="font-medium hover:underline">
            {{ $t('partnershipNotices.completeInfo.partnershipLink') }}
          </NuxtLink>
          - {{ $t('partnershipNotices.completeInfo.partnershipDescription') }}
        </li>
        <li>
          <NuxtLink
            :to="`/${eventSlug}/${partnershipId}/company`"
            class="font-medium hover:underline"
          >
            {{ $t('partnershipNotices.completeInfo.companyLink') }}
          </NuxtLink>
          - {{ $t('partnershipNotices.completeInfo.companyDescription') }}
        </li>
      </ul>
    </NoticeBlock>

    <!-- Message if awaiting payment (agreement signed but not paid) -->
    <NoticeBlock
      v-if="shouldShowAwaitingPayment"
      :title="$t('partnershipNotices.awaitingPayment.title')"
      icon="i-heroicons-clock"
      variant="warning"
    >
      <p>
        {{ $t('partnershipNotices.awaitingPayment.description') }}
      </p>
    </NoticeBlock>

    <!-- Message if payment received -->
    <NoticeBlock
      v-if="shouldShowPaymentReceived"
      :title="$t('partnershipNotices.paymentReceived.title')"
      icon="i-heroicons-check-circle"
      variant="success"
    >
      <p class="mb-3">
        {{ $t('partnershipNotices.paymentReceived.description') }}
      </p>
      <ul class="space-y-1 ml-5 list-disc">
        <li>
          <NuxtLink
            :to="`/${eventSlug}/${partnershipId}/job-offers`"
            class="font-medium hover:underline"
          >
            {{ $t('partnershipNotices.paymentReceived.jobOffersLink') }}
          </NuxtLink>
          - {{ $t('partnershipNotices.paymentReceived.jobOffersDescription') }}
        </li>
        <li>
          <NuxtLink
            :to="`/${eventSlug}/${partnershipId}/company`"
            class="font-medium hover:underline"
          >
            {{ $t('partnershipNotices.paymentReceived.companyLink') }}
          </NuxtLink>
          - {{ $t('partnershipNotices.paymentReceived.companyDescription') }}
        </li>
        <li>
          <NuxtLink
            :to="`/${eventSlug}/${partnershipId}/providers`"
            class="font-medium hover:underline"
          >
            {{ $t('partnershipNotices.paymentReceived.providersLink') }}
          </NuxtLink>
          - {{ $t('partnershipNotices.paymentReceived.providersDescription') }}
        </li>
      </ul>
    </NoticeBlock>
  </div>
</template>

<script setup lang="ts">
import type { PublicPartnership } from "~/types/partnership";

const props = defineProps<{
  partnership: PublicPartnership | null;
  eventSlug: string;
  partnershipId: string;
  loading?: boolean;
  error?: string | null;
}>();

/**
 * Determine if completion message should be shown
 */
const shouldShowCompletionMessage = computed(() => {
  if (props.loading || props.error || !props.partnership) {
    return false;
  }

  const hasDocumentsGenerated =
    props.partnership.quote_url || props.partnership.agreement_url;

  return (
    props.partnership.validated &&
    !props.partnership.paid &&
    !hasDocumentsGenerated
  );
});

/**
 * Determine if awaiting payment message should be shown
 */
const shouldShowAwaitingPayment = computed(() => {
  if (props.loading || props.error || !props.partnership) {
    return false;
  }

  return (
    props.partnership.agreement_signed_url &&
    !props.partnership.paid
  );
});

/**
 * Determine if payment received message should be shown
 */
const shouldShowPaymentReceived = computed(() => {
  if (props.loading || props.error || !props.partnership) {
    return false;
  }

  return props.partnership.paid;
});
</script>
