import { describe, it, expect } from "vitest";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import PartnershipStatusNotices from "./PartnershipStatusNotices.vue";
import type { PublicPartnership } from "~/types/partnership";

// Les tests utilisent les clés i18n car les traductions ne sont pas toujours chargées
const I18N_KEYS = {
  completeInfo: "partnershipNotices.completeInfo.title",
  awaitingPayment: "partnershipNotices.awaitingPayment.title",
  paymentReceived: "partnershipNotices.paymentReceived.title",
};

const createMockPartnership = (overrides: Partial<PublicPartnership> = {}): PublicPartnership =>
  ({
    id: "test-id",
    company_name: "Test Company",
    event_name: "Test Event",
    validated: false,
    paid: false,
    quote_url: null,
    agreement_url: null,
    agreement_signed_url: null,
    invoice_url: null,
    contact_name: "John Doe",
    contact_role: "Manager",
    emails: ["test@example.com"],
    phone: "0123456789",
    language: "fr",
    selected_pack_id: "pack-1",
    selected_pack_name: "Gold",
    validated_pack_id: null,
    validated_pack_name: null,
    suggested_pack_id: null,
    suggested_pack_name: null,
    ...overrides,
  }) as PublicPartnership;

describe("PartnershipStatusNotices", () => {
  describe("Completion message", () => {
    it("shows completion message when validated but not paid and no documents", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
        quote_url: null,
        agreement_url: null,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).toContain(I18N_KEYS.completeInfo);
    });

    it("does not show completion message when loading", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: true,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.completeInfo);
    });

    it("does not show completion message when there is an error", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: "Some error",
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.completeInfo);
    });

    it("does not show completion message when documents are generated", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
        quote_url: "https://example.com/quote.pdf",
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.completeInfo);
    });

    it("does not show completion message when already paid", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.completeInfo);
    });
  });

  describe("Awaiting payment message", () => {
    it("shows awaiting payment message when agreement is signed but not paid", async () => {
      const partnership = createMockPartnership({
        agreement_signed_url: "https://example.com/signed.pdf",
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).toContain(I18N_KEYS.awaitingPayment);
    });

    it("does not show awaiting payment when no signed agreement", async () => {
      const partnership = createMockPartnership({
        agreement_signed_url: null,
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.awaitingPayment);
    });

    it("does not show awaiting payment when already paid", async () => {
      const partnership = createMockPartnership({
        agreement_signed_url: "https://example.com/signed.pdf",
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.awaitingPayment);
    });

    it("does not show awaiting payment when loading", async () => {
      const partnership = createMockPartnership({
        agreement_signed_url: "https://example.com/signed.pdf",
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: true,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.awaitingPayment);
    });
  });

  describe("Payment received message", () => {
    it("shows payment received message when paid", async () => {
      const partnership = createMockPartnership({
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).toContain(I18N_KEYS.paymentReceived);
    });

    it("does not show payment received when not paid", async () => {
      const partnership = createMockPartnership({
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.paymentReceived);
    });

    it("does not show payment received when loading", async () => {
      const partnership = createMockPartnership({
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: true,
          error: null,
        },
      });

      expect(wrapper.text()).not.toContain(I18N_KEYS.paymentReceived);
    });

    it("contains links to job offers, company and providers pages", async () => {
      const partnership = createMockPartnership({
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      const links = wrapper.findAll("a");
      const hrefs = links.map((link) => link.attributes("href"));

      expect(hrefs).toContain("/test-event/test-id/job-offers");
      expect(hrefs).toContain("/test-event/test-id/company");
      expect(hrefs).toContain("/test-event/test-id/providers");
    });
  });

  describe("No partnership", () => {
    it("shows nothing when partnership is null", async () => {
      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership: null,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.text()).toBe("");
    });
  });

  describe("Multiple states", () => {
    it("can show both completion and awaiting payment messages", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
        agreement_signed_url: "https://example.com/signed.pdf",
        quote_url: null,
        agreement_url: null,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      // Should show both messages
      expect(wrapper.text()).toContain(I18N_KEYS.completeInfo);
      expect(wrapper.text()).toContain(I18N_KEYS.awaitingPayment);
    });
  });

  describe("Link generation", () => {
    it("generates correct links with event slug and partnership id", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
        quote_url: null,
        agreement_url: null,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "my-event",
          partnershipId: "my-partnership-123",
          loading: false,
          error: null,
        },
      });

      const links = wrapper.findAll("a");
      const hrefs = links.map((link) => link.attributes("href"));

      expect(hrefs).toContain("/my-event/my-partnership-123");
      expect(hrefs).toContain("/my-event/my-partnership-123/company");
    });
  });

  describe("NoticeBlock variants", () => {
    it("uses info variant for completion message", async () => {
      const partnership = createMockPartnership({
        validated: true,
        paid: false,
        quote_url: null,
        agreement_url: null,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.find(".bg-blue-50").exists()).toBe(true);
    });

    it("uses warning variant for awaiting payment message", async () => {
      const partnership = createMockPartnership({
        agreement_signed_url: "https://example.com/signed.pdf",
        paid: false,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.find(".bg-amber-50").exists()).toBe(true);
    });

    it("uses success variant for payment received message", async () => {
      const partnership = createMockPartnership({
        paid: true,
      });

      const wrapper = await mountSuspended(PartnershipStatusNotices, {
        props: {
          partnership,
          eventSlug: "test-event",
          partnershipId: "test-id",
          loading: false,
          error: null,
        },
      });

      expect(wrapper.find(".bg-green-50").exists()).toBe(true);
    });
  });
});
