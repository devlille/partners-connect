import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, nextTick, ref } from "vue";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { useSendEmail, type UseSendEmailOptions } from "~/composables/useSendEmail";
import type { PostPartnershipEmailParams } from "~/utils/api";

vi.mock("~/utils/api", () => ({
  postPartnershipEmail: vi.fn(),
}));

const DEFAULT_OPTIONS: UseSendEmailOptions = {
  orgSlug: "devlille",
  eventSlug: "devlille-2026",
};

async function buildComposable(options: UseSendEmailOptions = DEFAULT_OPTIONS) {
  let composable!: ReturnType<typeof useSendEmail>;
  await mountSuspended(
    defineComponent({
      setup() {
        composable = useSendEmail(options);
        return {};
      },
      render: () => null,
    }),
  );
  return composable;
}

describe("useSendEmail", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("initial state", () => {
    it("initializes with empty form and no status", async () => {
      const { formData, sending, error, successMessage } =
        await buildComposable();

      expect(formData.value.subject).toBe("");
      expect(formData.value.body).toBe("");
      expect(sending.value).toBe(false);
      expect(error.value).toBeNull();
      expect(successMessage.value).toBeNull();
    });
  });

  describe("isFormValid", () => {
    it("is false when subject and body are empty", async () => {
      const { isFormValid } = await buildComposable();
      expect(isFormValid.value).toBe(false);
    });

    it("is false when only subject is filled", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.subject = "Hello";
      expect(isFormValid.value).toBe(false);
    });

    it("is false when only body is filled", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.body = "Some content";
      expect(isFormValid.value).toBe(false);
    });

    it("is true when both subject and body are filled", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Some content";
      expect(isFormValid.value).toBe(true);
    });

    it("is false when body contains only HTML tags with no visible text", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "<p><br></p>";
      expect(isFormValid.value).toBe(false);
    });

    it("is true when body contains HTML with visible text", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "<p>Some content</p>";
      expect(isFormValid.value).toBe(true);
    });

    it("is false when subject is only whitespace", async () => {
      const { formData, isFormValid } = await buildComposable();
      formData.value.subject = "   ";
      formData.value.body = "Some content";
      expect(isFormValid.value).toBe(false);
    });
  });

  describe("hasFilters", () => {
    it("is false when no filterParams provided", async () => {
      const { hasFilters } = await buildComposable();
      expect(hasFilters.value).toBe(false);
    });

    it("is false when filterParams is an empty object", async () => {
      const { hasFilters } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams: {},
      });
      expect(hasFilters.value).toBe(false);
    });

    it("is false when all filterParams values are null or undefined", async () => {
      const { hasFilters } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams: { "filter[validated]": undefined, "filter[paid]": undefined },
      });
      expect(hasFilters.value).toBe(false);
    });

    it("is true when at least one filterParam has a value", async () => {
      const { hasFilters } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams: { "filter[validated]": true },
      });
      expect(hasFilters.value).toBe(true);
    });

    it("reacts to getter changes", async () => {
      const filterRef = ref<PostPartnershipEmailParams | undefined>(undefined);
      const { hasFilters } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams: () => filterRef.value,
      });

      expect(hasFilters.value).toBe(false);

      filterRef.value = { "filter[validated]": true };
      await nextTick();

      expect(hasFilters.value).toBe(true);
    });
  });

  describe("resetForm", () => {
    it("clears form, error, and successMessage", async () => {
      const { formData, error, successMessage, resetForm } =
        await buildComposable();

      formData.value.subject = "Hello";
      formData.value.body = "Body";
      error.value = "Some error";
      successMessage.value = "Success!";

      resetForm();

      expect(formData.value.subject).toBe("");
      expect(formData.value.body).toBe("");
      expect(error.value).toBeNull();
      expect(successMessage.value).toBeNull();
    });
  });

  describe("sendEmail", () => {
    it("returns null and does not call API when form is invalid", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      const { sendEmail } = await buildComposable();

      const result = await sendEmail();

      expect(result).toBeNull();
      expect(postPartnershipEmail).not.toHaveBeenCalled();
    });

    it("returns null and does not call API when already sending", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");

      const { formData, sending, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";
      sending.value = true;

      const result = await sendEmail();

      expect(result).toBeNull();
      expect(postPartnershipEmail).not.toHaveBeenCalled();
    });

    it("calls postPartnershipEmail with trimmed values", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 3 },
      } as any);

      const { formData, sendEmail } = await buildComposable();
      formData.value.subject = "  Hello  ";
      formData.value.body = "  Body content  ";

      await sendEmail();

      expect(postPartnershipEmail).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        { subject: "Hello", body: "Body content" },
        undefined,
      );
    });

    it("passes filterParams to the API call", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 2 },
      } as any);

      const filterParams: PostPartnershipEmailParams = { "filter[validated]": true, "filter[declined]": false };
      const { formData, sendEmail } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams,
      });
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(postPartnershipEmail).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        expect.any(Object),
        filterParams,
      );
    });

    it("uses the getter current value at send time", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 5 },
      } as any);

      const filterRef = ref<PostPartnershipEmailParams | undefined>(undefined);
      const { formData, sendEmail } = await buildComposable({
        ...DEFAULT_OPTIONS,
        filterParams: () => filterRef.value,
      });
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      filterRef.value = { "filter[validated]": true };
      await sendEmail();

      expect(postPartnershipEmail).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        expect.any(Object),
        { "filter[validated]": true },
      );
    });

    it("returns recipient count and sets successMessage on success", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 7 },
      } as any);

      const { formData, successMessage, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      const result = await sendEmail();

      expect(result).toBe(7);
      expect(successMessage.value).toBeTruthy();
    });

    it("calls onSuccess callback with recipient count", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 4 },
      } as any);

      const onSuccess = vi.fn();
      const { formData, sendEmail } = await buildComposable({
        ...DEFAULT_OPTIONS,
        onSuccess,
      });
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(onSuccess).toHaveBeenCalledWith(4);
    });

    it("sets error message from API response and returns null on failure", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      const apiError = Object.assign(new Error("API error"), {
        response: { data: { message: "Mailjet not configured" } },
      });
      vi.mocked(postPartnershipEmail).mockRejectedValue(apiError);

      const { formData, error, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      const result = await sendEmail();

      expect(result).toBeNull();
      expect(error.value).toBe("Mailjet not configured");
    });

    it("falls back to i18n error key when API provides no message", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockRejectedValue(
        new Error("Network error"),
      );

      const { formData, error, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(error.value).toBeTruthy();
    });

    it("calls onError callback on failure", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      const apiError = new Error("Something went wrong");
      vi.mocked(postPartnershipEmail).mockRejectedValue(apiError);

      const onError = vi.fn();
      const { formData, sendEmail } = await buildComposable({
        ...DEFAULT_OPTIONS,
        onError,
      });
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(onError).toHaveBeenCalledWith(apiError);
    });

    it("resets sending to false after success", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 1 },
      } as any);

      const { formData, sending, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(sending.value).toBe(false);
    });

    it("resets sending to false after failure", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockRejectedValue(new Error("fail"));

      const { formData, sending, sendEmail } = await buildComposable();
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(sending.value).toBe(false);
    });

    it("clears previous error before sending", async () => {
      const { postPartnershipEmail } = await import("~/utils/api");
      vi.mocked(postPartnershipEmail).mockResolvedValue({
        data: { recipients: 1 },
      } as any);

      const { formData, error, sendEmail } = await buildComposable();
      error.value = "Previous error";
      formData.value.subject = "Hello";
      formData.value.body = "Body";

      await sendEmail();

      expect(error.value).toBeNull();
    });
  });
});
