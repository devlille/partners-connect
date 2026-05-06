import { describe, it, expect } from "vitest";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import BoothLocationsList from "./BoothLocationsList.vue";
import type { BoothSponsor } from "~/composables/useBoothSponsors";

const SPONSORS: BoothSponsor[] = [
  { id: "p-gold", companyName: "Acme", packName: "Pack Gold", boothLocation: "A1" },
  { id: "p-silver", companyName: "Beeko", packName: "Pack Silver", boothLocation: null },
];

const ORG_SLUG = "devlille";
const EVENT_SLUG = "devlille-2026";

describe("BoothLocationsList", () => {
  it("renders one row per sponsor with company, pack and location", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: SPONSORS,
        loading: false,
        error: null,
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    const text = wrapper.text();
    expect(text).toContain("Acme");
    expect(text).toContain("Pack Gold");
    expect(text).toContain("A1");
    expect(text).toContain("Beeko");
    expect(text).toContain("Pack Silver");

    const rows = wrapper.findAll("tbody tr");
    expect(rows).toHaveLength(2);
  });

  it("shows the 'unassigned' badge when boothLocation is null", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: [SPONSORS[1]!],
        loading: false,
        error: null,
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    expect(wrapper.text()).toContain("Unassigned");
  });

  it("renders the empty state when sponsors is an empty array", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: [],
        loading: false,
        error: null,
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    expect(wrapper.find("tbody tr").exists()).toBe(false);
    expect(wrapper.text()).toContain("No validated Pack Silver or Pack Gold sponsor");
  });

  it("renders an error block when the error prop is set", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: [],
        loading: false,
        error: "Boom",
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    expect(wrapper.find("[role='alert']").exists()).toBe(true);
    expect(wrapper.text()).toContain("Boom");
  });

  it("renders a loading indicator when loading is true", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: [],
        loading: true,
        error: null,
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    expect(wrapper.find("[data-testid='booth-locations-loading']").exists()).toBe(true);
  });

  it("links each company to its sponsor detail page", async () => {
    const wrapper = await mountSuspended(BoothLocationsList, {
      props: {
        sponsors: SPONSORS,
        loading: false,
        error: null,
        orgSlug: ORG_SLUG,
        eventSlug: EVENT_SLUG,
      },
    });

    const links = wrapper.findAll("a");
    const hrefs = links.map((l) => l.attributes("href"));
    expect(hrefs).toContain(`/orgs/${ORG_SLUG}/events/${EVENT_SLUG}/sponsors/p-gold`);
    expect(hrefs).toContain(`/orgs/${ORG_SLUG}/events/${EVENT_SLUG}/sponsors/p-silver`);
  });
});
