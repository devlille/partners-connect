import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, nextTick } from "vue";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { useBoothSponsors } from "~/composables/useBoothSponsors";

vi.mock("~/utils/api", () => ({
  getOrgsEventsPacks: vi.fn(),
  getOrgsEventsBoothPlan: vi.fn(),
}));

const ORG_SLUG = "devlille";
const EVENT_SLUG = "devlille-2026";

const SILVER_PACK = { id: "silver-id", name: "Pack Silver", base_price: 100 };
const GOLD_PACK = { id: "gold-id", name: "Pack Gold", base_price: 200 };
const BRONZE_PACK = { id: "bronze-id", name: "Pack Bronze", base_price: 50 };

function buildBoothLocationItem(overrides: {
  id: string;
  validated_pack_id?: string | null;
  company_name?: string;
  booth_location?: string | null;
}) {
  return {
    partnership: {
      id: overrides.id,
      company_name: overrides.company_name ?? `Company ${overrides.id}`,
      event_name: "DevLille 2026",
      contact: { display_name: "X", email: "x@x.fr" },
      language: "fr",
      created_at: "2026-01-01T00:00:00Z",
      validated_pack_id: overrides.validated_pack_id ?? null,
    },
    booth_location: overrides.booth_location ?? null,
  };
}

async function buildComposable(orgSlug = ORG_SLUG, eventSlug = EVENT_SLUG) {
  let composable!: ReturnType<typeof useBoothSponsors>;
  await mountSuspended(
    defineComponent({
      setup() {
        composable = useBoothSponsors(orgSlug, eventSlug);
        return {};
      },
      render: () => null,
    }),
  );
  return composable;
}

describe("useBoothSponsors", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("returns only Silver/Gold sponsors and skips Bronze / unvalidated ones", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK, BRONZE_PACK],
    } as any);

    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({
      data: [
        buildBoothLocationItem({
          id: "p-silver",
          validated_pack_id: "silver-id",
          company_name: "Acme",
          booth_location: "A1",
        }),
        buildBoothLocationItem({
          id: "p-gold",
          validated_pack_id: "gold-id",
          company_name: "Beeko",
          booth_location: "B2",
        }),
        buildBoothLocationItem({ id: "p-bronze", validated_pack_id: "bronze-id" }),
        buildBoothLocationItem({ id: "p-none", validated_pack_id: null }),
      ],
    } as any);

    const { sponsors, loading } = await buildComposable();

    while (loading.value) await nextTick();

    expect(sponsors.value.map((s) => s.id).sort()).toEqual(["p-gold", "p-silver"]);
    const silver = sponsors.value.find((s) => s.id === "p-silver")!;
    expect(silver.companyName).toBe("Acme");
    expect(silver.packName).toBe("Pack Silver");
    expect(silver.boothLocation).toBe("A1");
  });

  it("sorts sponsors by boothLocation alphabetically with unassigned at the end", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK],
    } as any);

    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({
      data: [
        buildBoothLocationItem({
          id: "p1",
          validated_pack_id: "silver-id",
          booth_location: "C3",
        }),
        buildBoothLocationItem({
          id: "p2",
          validated_pack_id: "silver-id",
          booth_location: null,
        }),
        buildBoothLocationItem({
          id: "p3",
          validated_pack_id: "gold-id",
          booth_location: "A1",
        }),
        buildBoothLocationItem({
          id: "p4",
          validated_pack_id: "gold-id",
          booth_location: "B2",
        }),
      ],
    } as any);

    const { sponsors, loading } = await buildComposable();
    while (loading.value) await nextTick();

    expect(sponsors.value.map((s) => `${s.id}:${s.boothLocation ?? "-"}`)).toEqual([
      "p3:A1",
      "p4:B2",
      "p1:C3",
      "p2:-",
    ]);
  });

  it("toggles loading from true to false", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [SILVER_PACK] } as any);
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({ data: [] } as any);

    const { loading } = await buildComposable();
    while (loading.value) await nextTick();
    expect(loading.value).toBe(false);
  });

  it("returns an empty list when no Silver/Gold pack exists", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [BRONZE_PACK] } as any);
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({
      data: [buildBoothLocationItem({ id: "p1", validated_pack_id: "bronze-id" })],
    } as any);

    const { sponsors, loading } = await buildComposable();
    while (loading.value) await nextTick();

    expect(sponsors.value).toEqual([]);
  });

  it("sets error and keeps sponsors empty on API failure", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockRejectedValue(new Error("boom"));
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({ data: [] } as any);

    const { sponsors, loading, error } = await buildComposable();
    while (loading.value) await nextTick();

    expect(sponsors.value).toEqual([]);
    expect(error.value).not.toBeNull();
  });

  it("calls the booth-plan endpoint with the org and event slugs", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [] } as any);
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({ data: [] } as any);

    const { loading } = await buildComposable();
    while (loading.value) await nextTick();

    expect(getOrgsEventsBoothPlan).toHaveBeenCalledWith(ORG_SLUG, EVENT_SLUG);
  });

  it("exposes a refresh function that re-fetches the data", async () => {
    const { getOrgsEventsPacks, getOrgsEventsBoothPlan } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [] } as any);
    vi.mocked(getOrgsEventsBoothPlan).mockResolvedValue({ data: [] } as any);

    const { loading, refresh } = await buildComposable();
    while (loading.value) await nextTick();

    expect(getOrgsEventsPacks).toHaveBeenCalledTimes(1);
    expect(getOrgsEventsBoothPlan).toHaveBeenCalledTimes(1);

    await refresh();

    expect(getOrgsEventsPacks).toHaveBeenCalledTimes(2);
    expect(getOrgsEventsBoothPlan).toHaveBeenCalledTimes(2);
  });
});
