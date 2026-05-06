import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, nextTick } from "vue";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { useBoothSponsors } from "~/composables/useBoothSponsors";
import BoothLocationsList from "~/components/booth/BoothLocationsList.vue";

vi.mock("~/utils/api", () => ({
  getOrgsEventsPacks: vi.fn(),
  getOrgsEventsPartnership: vi.fn(),
  getEventsPartnershipDetailed: vi.fn(),
}));

const ORG_SLUG = "devlille";
const EVENT_SLUG = "devlille-2026";

const SILVER_PACK = { id: "silver-id", name: "Pack Silver", base_price: 100 };
const GOLD_PACK = { id: "gold-id", name: "Pack Gold", base_price: 200 };

describe("booth-plan page integration — sponsors section", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("wires the composable to BoothLocationsList and renders fetched sponsors", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership, getEventsPartnershipDetailed } =
      await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK],
    } as any);
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: {
        items: [
          {
            id: "p-gold",
            company_name: "Acme",
            event_name: "DevLille 2026",
            contact: { display_name: "X", email: "x@x.fr" },
            language: "fr",
            created_at: "2026-01-01T00:00:00Z",
            validated_pack_id: "gold-id",
          },
        ],
        page: 1,
        page_size: 100,
        total: 1,
      },
    } as any);
    vi.mocked(getEventsPartnershipDetailed).mockResolvedValue({
      data: {
        partnership: {
          id: "p-gold",
          contact_name: "X",
          contact_role: "Y",
          language: "fr",
          emails: [],
          validated_pack: {
            id: "gold-id",
            name: "Pack Gold",
            base_price: 200,
            required_options: [],
            optional_options: [],
            total_price: 200,
          },
          process_status: {},
          created_at: "2026-01-01T00:00:00Z",
          currency: "EUR",
          booth_location: "A1",
        },
        company: { name: "Acme" },
        event: {},
        organisation: {},
      },
    } as any);

    const Wrapper = defineComponent({
      components: { BoothLocationsList },
      setup() {
        const { sponsors, loading, error } = useBoothSponsors(ORG_SLUG, EVENT_SLUG);
        return { sponsors, loading, error };
      },
      template: `
        <BoothLocationsList
          :sponsors="sponsors"
          :loading="loading"
          :error="error"
          org-slug="${ORG_SLUG}"
          event-slug="${EVENT_SLUG}"
        />
      `,
    });

    const wrapper = await mountSuspended(Wrapper);

    // Wait for the composable's load() promise chain to resolve.
    while (wrapper.text().includes("Loading")) await nextTick();
    await nextTick();

    expect(wrapper.text()).toContain("Acme");
    expect(wrapper.text()).toContain("Pack Gold");
    expect(wrapper.text()).toContain("A1");

    expect(getOrgsEventsPacks).toHaveBeenCalledWith(ORG_SLUG, EVENT_SLUG);
    expect(getOrgsEventsPartnership).toHaveBeenCalledWith(
      ORG_SLUG,
      EVENT_SLUG,
      expect.objectContaining({ page_size: 100 }),
    );
    expect(getEventsPartnershipDetailed).toHaveBeenCalledWith(EVENT_SLUG, "p-gold");
  });
});
