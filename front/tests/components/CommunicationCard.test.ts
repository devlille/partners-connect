import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import CommunicationCard from "~/components/CommunicationCard.vue";

const baseItem = {
  partnership_id: "123",
  company_name: "Acme Corp",
  publication_date: null,
  support_url: null,
};

describe("CommunicationCard", () => {
  describe("Schedule button label", () => {
    it("shows 'Planifier' when unplanned and no date", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: baseItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Planifier");
    });

    it("shows 'Modifier la date' when planned with a date", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...baseItem, publication_date: "2026-06-01T00:00:00" },
          status: "planned",
        },
      });
      expect(wrapper.text()).toContain("Modifier la date");
    });

    it("shows 'Reprogrammer' when done", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...baseItem, publication_date: "2026-01-01T00:00:00" },
          status: "done",
        },
      });
      expect(wrapper.text()).toContain("Reprogrammer");
    });
  });

  describe("Schedule button visibility", () => {
    it("shows schedule button for done items", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...baseItem, publication_date: "2026-01-01T00:00:00" },
          status: "done",
        },
      });
      const buttons = wrapper.findAll("button");
      const scheduleButton = buttons.find((b) =>
        b.text().includes("Reprogrammer")
      );
      expect(scheduleButton).toBeDefined();
    });

    it("emits schedule event when schedule button clicked for done item", async () => {
      const item = { ...baseItem, publication_date: "2026-01-01T00:00:00" };
      const wrapper = mount(CommunicationCard, {
        props: { item, status: "done" },
      });
      const buttons = wrapper.findAll("button");
      const scheduleButton = buttons.find((b) =>
        b.text().includes("Reprogrammer")
      );
      await scheduleButton?.trigger("click");
      expect(wrapper.emitted("schedule")).toBeTruthy();
      expect(wrapper.emitted("schedule")?.[0]).toEqual([item]);
    });
  });

  describe("Status badge", () => {
    it("shows 'Terminée' badge for done status", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...baseItem, publication_date: "2026-01-01T00:00:00" },
          status: "done",
        },
      });
      expect(wrapper.text()).toContain("Terminée");
    });

    it("shows 'Planifiée' badge for planned status", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...baseItem, publication_date: "2026-06-01T00:00:00" },
          status: "planned",
        },
      });
      expect(wrapper.text()).toContain("Planifiée");
    });

    it("shows 'Non planifiée' badge for unplanned status", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: baseItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Non planifiée");
    });
  });
});
