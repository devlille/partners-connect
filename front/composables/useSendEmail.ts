import { postPartnershipEmail, type PostPartnershipEmailParams } from "~/utils/api";

export interface SendEmailFormData {
  subject: string;
  body: string;
}

export interface UseSendEmailOptions {
  orgSlug: string;
  eventSlug: string;
  filterParams?: PostPartnershipEmailParams;
  onSuccess?: (recipientCount: number) => void;
  onError?: (error: Error) => void;
}

export function useSendEmail(options: UseSendEmailOptions) {
  const { t } = useI18n();

  const formData = ref<SendEmailFormData>({
    subject: "",
    body: "",
  });

  const sending = ref(false);
  const error = ref<string | null>(null);
  const successMessage = ref<string | null>(null);

  const isFormValid = computed(() => {
    const subject = formData.value.subject.trim();
    const body = stripHtmlTags(formData.value.body).trim();
    return subject.length > 0 && body.length > 0;
  });

  const hasFilters = computed(() => {
    if (!options.filterParams) return false;
    return Object.values(options.filterParams).some((v) => v !== undefined && v !== null);
  });

  /**
   * Strip HTML tags to check if content is empty
   */
  function stripHtmlTags(html: string): string {
    return html.replace(/<[^>]*>/g, "");
  }

  /**
   * Reset form to initial state
   */
  function resetForm() {
    formData.value = {
      subject: "",
      body: "",
    };
    error.value = null;
    successMessage.value = null;
  }

  /**
   * Send email to partnerships
   */
  async function sendEmail(): Promise<number | null> {
    if (!isFormValid.value || sending.value) return null;

    sending.value = true;
    error.value = null;
    successMessage.value = null;

    try {
      const response = await postPartnershipEmail(
        options.orgSlug,
        options.eventSlug,
        {
          subject: formData.value.subject.trim(),
          body: formData.value.body.trim(),
        },
        options.filterParams,
      );

      const recipientCount = response.data.recipients;
      successMessage.value = t("email.modal.success", { count: recipientCount });
      options.onSuccess?.(recipientCount);
      return recipientCount;
    } catch (err: unknown) {
      console.error("Failed to send email:", err);
      const errorMessage =
        err instanceof Error && "response" in err ? (err as any).response?.data?.message : null;
      error.value = errorMessage || t("email.modal.error");
      options.onError?.(err instanceof Error ? err : new Error(String(err)));
      return null;
    } finally {
      sending.value = false;
    }
  }

  return {
    formData,
    sending,
    error,
    successMessage,
    isFormValid,
    hasFilters,
    resetForm,
    sendEmail,
  };
}
