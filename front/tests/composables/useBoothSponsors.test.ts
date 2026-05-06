import { describe, it, expect, vi, beforeEach } from "vitest";
import { defineComponent, nextTick } from "vue";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { useBoothSponsors } from "~/composables/useBoothSponsors";

vi.mock("~/utils/api", () => ({
  getOrgsEventsPacks: vi.fn(),
  getOrgsEventsPartnership: vi.fn(),
  getEventsPartnershipDetailed: vi.fn(),
}));

const ORG_SLUG = "devlille";
const EVENT_SLUG = "devlille-2026";

const SILVER_PACK = { id: "silver-id", name: "Pack Silver", base_price: 100 };
const GOLD_PACK = { id: "gold-id", name: "Pack Gold", base_price: 200 };
const BRONZE_PACK = { id: "bronze-id", name: "Pack Bronze", base_price: 50 };

function buildPartnershipItem(overrides: {
  id: string;
  validated_pack_id?: string | null;
  company_name?: string;
}) {
  return {
    id: overrides.id,
    company_name: overrides.company_name ?? `Company ${overrides.id}`,
    event_name: "DevLille 2026",
    contact: { display_name: "X", email: "x@x.fr" },
    language: "fr",
    created_at: "2026-01-01T00:00:00Z",
    validated_pack_id: overrides.validated_pack_id ?? null,
  };
}

function buildDetailResponse(
  id: string,
  pack: { id: string; name: string },
  boothLocation: string | null,
  companyName?: string,
) {
  return {
    data: {
      partnership: {
        id,
        contact_name: "X",
        contact_role: "Y",
        language: "fr",
        emails: [],
        validated_pack: {
          id: pack.id,
          name: pack.name,
          base_price: 100,
          required_options: [],
          optional_options: [],
          total_price: 100,
        },
        process_status: {},
        created_at: "2026-01-01T00:00:00Z",
        currency: "EUR",
        booth_location: boothLocation,
      },
      company: { name: companyName ?? `Company ${id}` },
      event: {},
      organisation: {},
    },
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
    const { getOrgsEventsPacks, getOrgsEventsPartnership, getEventsPartnershipDetailed } =
      await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK, BRONZE_PACK],
    } as any);

    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: {
        items: [
          buildPartnershipItem({ id: "p-silver", validated_pack_id: "silver-id" }),
          buildPartnershipItem({ id: "p-gold", validated_pack_id: "gold-id" }),
          buildPartnershipItem({ id: "p-bronze", validated_pack_id: "bronze-id" }),
          buildPartnershipItem({ id: "p-none", validated_pack_id: null }),
        ],
        page: 1,
        page_size: 100,
        total: 4,
      },
    } as any);

    vi.mocked(getEventsPartnershipDetailed).mockImplementation((_eventSlug, id) => {
      if (id === "p-silver")
        return Promise.resolve(buildDetailResponse("p-silver", SILVER_PACK, "A1", "Acme") as any);
      if (id === "p-gold")
        return Promise.resolve(buildDetailResponse("p-gold", GOLD_PACK, "B2", "Beeko") as any);
      throw new Error(`Unexpected id ${id}`);
    });

    const { sponsors, loading } = await buildComposable();

    while (loading.value) await nextTick();

    expect(getEventsPartnershipDetailed).toHaveBeenCalledTimes(2);
    expect(sponsors.value.map((s) => s.id).sort()).toEqual(["p-gold", "p-silver"]);
    const silver = sponsors.value.find((s) => s.id === "p-silver")!;
    expect(silver.companyName).toBe("Acme");
    expect(silver.packName).toBe("Pack Silver");
    expect(silver.boothLocation).toBe("A1");
  });

  it("sorts sponsors by boothLocation alphabetically with unassigned at the end", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership, getEventsPartnershipDetailed } =
      await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({
      data: [SILVER_PACK, GOLD_PACK],
    } as any);

    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: {
        items: [
          buildPartnershipItem({ id: "p1", validated_pack_id: "silver-id" }),
          buildPartnershipItem({ id: "p2", validated_pack_id: "silver-id" }),
          buildPartnershipItem({ id: "p3", validated_pack_id: "gold-id" }),
          buildPartnershipItem({ id: "p4", validated_pack_id: "gold-id" }),
        ],
        page: 1,
        page_size: 100,
        total: 4,
      },
    } as any);

    vi.mocked(getEventsPartnershipDetailed).mockImplementation((_eventSlug, id) => {
      const map: Record<string, string | null> = {
        p1: "C3",
        p2: null,
        p3: "A1",
        p4: "B2",
      };
      const pack = id === "p3" || id === "p4" ? GOLD_PACK : SILVER_PACK;
      return Promise.resolve(buildDetailResponse(id, pack, map[id]) as any);
    });

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
    const { getOrgsEventsPacks, getOrgsEventsPartnership, getEventsPartnershipDetailed } =
      await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [SILVER_PACK] } as any);
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: { items: [], page: 1, page_size: 100, total: 0 },
    } as any);
    vi.mocked(getEventsPartnershipDetailed).mockResolvedValue(
      buildDetailResponse("x", SILVER_PACK, null) as any,
    );

    const { loading } = await buildComposable();
    while (loading.value) await nextTick();
    expect(loading.value).toBe(false);
  });

  it("returns an empty list and skips detailed calls when no Silver/Gold pack exists", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership, getEventsPartnershipDetailed } =
      await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [BRONZE_PACK] } as any);
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: {
        items: [buildPartnershipItem({ id: "p1", validated_pack_id: "bronze-id" })],
        page: 1,
        page_size: 100,
        total: 1,
      },
    } as any);

    const { sponsors, loading } = await buildComposable();
    while (loading.value) await nextTick();

    expect(sponsors.value).toEqual([]);
    expect(getEventsPartnershipDetailed).not.toHaveBeenCalled();
  });

  it("sets error and keeps sponsors empty on API failure", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockRejectedValue(new Error("boom"));
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: { items: [], page: 1, page_size: 100, total: 0 },
    } as any);

    const { sponsors, loading, error } = await buildComposable();
    while (loading.value) await nextTick();

    expect(sponsors.value).toEqual([]);
    expect(error.value).not.toBeNull();
  });

  it("requests partnerships with a large page size to avoid pagination", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [] } as any);
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: { items: [], page: 1, page_size: 100, total: 0 },
    } as any);

    const { loading } = await buildComposable();
    while (loading.value) await nextTick();

    expect(getOrgsEventsPartnership).toHaveBeenCalledWith(
      ORG_SLUG,
      EVENT_SLUG,
      expect.objectContaining({ page_size: 100 }),
    );
  });

  it("exposes a refresh function that re-fetches the data", async () => {
    const { getOrgsEventsPacks, getOrgsEventsPartnership } = await import("~/utils/api");

    vi.mocked(getOrgsEventsPacks).mockResolvedValue({ data: [] } as any);
    vi.mocked(getOrgsEventsPartnership).mockResolvedValue({
      data: { items: [], page: 1, page_size: 100, total: 0 },
    } as any);

    const { loading, refresh } = await buildComposable();
    while (loading.value) await nextTick();

    expect(getOrgsEventsPacks).toHaveBeenCalledTimes(1);

    await refresh();

    expect(getOrgsEventsPacks).toHaveBeenCalledTimes(2);
  });
});
