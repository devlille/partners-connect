import { toValue, type MaybeRefOrGetter } from "vue";
import { postPartnershipEmailDraft } from "~/utils/api";

export interface UseDraftEmailOptions {
  orgSlug: string;
  eventSlug: string;
  partnershipIds: MaybeRefOrGetter<string[]>;
  onSuccess?: (draft: string) => void;
  onError?: (error: Error) => void;
}

export function useDraftEmail(options: UseDraftEmailOptions) {
  const { t } = useI18n();

  const prompt = ref<string>("");
  const generating = ref(false);
  const error = ref<string | null>(null);

  const canGenerate = computed(() => {
    return prompt.value.trim().length > 0 && toValue(options.partnershipIds).length > 0;
  });

  function resetDraftState() {
    prompt.value = "";
    error.value = null;
  }

  async function generate(): Promise<string | null> {
    if (!canGenerate.value || generating.value) return null;

    generating.value = true;
    error.value = null;

    try {
      const response = await postPartnershipEmailDraft(options.orgSlug, options.eventSlug, {
        partnership_ids: toValue(options.partnershipIds),
        prompt: prompt.value.trim(),
      });
      const draft = response.data.draft;
      options.onSuccess?.(draft);
      return draft;
    } catch (err: unknown) {
      console.error("Failed to generate email draft:", err);
      const errorMessage =
        err instanceof Error && "response" in err ? (err as any).response?.data?.message : null;
      error.value = errorMessage || t("email.modal.generateError");
      options.onError?.(err instanceof Error ? err : new Error(String(err)));
      return null;
    } finally {
      generating.value = false;
    }
  }

  return {
    prompt,
    generating,
    error,
    canGenerate,
    resetDraftState,
    generate,
  };
}
