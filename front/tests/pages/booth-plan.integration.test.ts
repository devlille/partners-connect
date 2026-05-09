import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, nextTick } from "vue";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { useBoothSponsors } from "~/composables/useBoothSponsors";
import BoothLocationsList from "~/components/booth/BoothLocationsList.vue";

vi.mock("~/utils/api", () => ({
  getOrgsEventsPacks: vi.fn(),
  getOrgsEventsBoothPlan: vi.fn(),
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
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK],
    } as any);
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({
      data: [
        {
          partnership: {
            id: "p-gold",
            company_name: "Acme",
            event_name: "DevLille 2026",
            contact: { display_name: "X", email: "x@x.fr" },
            language: "fr",
            created_at: "2026-01-01T00:00:00Z",
            validated_pack_id: "gold-id",
          },
          booth_location: "A1",
        },
      ],
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
    expect(getOrgsEventsBoothPlan).toHaveBeenCalledWith(ORG_SLUG, EVENT_SLUG);
  });
});
