import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import CommunicationCard from "~/components/CommunicationCard.vue";

const partnershipItem = {
  id: "entry-1",
  partnership_id: "123",
  title: "Acme Corp",
  publication_date: null,
  support_url: null,
};

const standaloneItem = {
  id: "entry-2",
  partnership_id: null,
  title: "Post LinkedIn DevLille 2026",
  publication_date: null,
  support_url: null,
};

describe("CommunicationCard", () => {
  describe("Title display", () => {
    it("displays item title", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: partnershipItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Acme Corp");
    });

    it("displays standalone item title", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: standaloneItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Post LinkedIn DevLille 2026");
    });
  });

  describe("Schedule button label (partnership-linked)", () => {
    it("shows 'Planifier' when unplanned and no date", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: partnershipItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Planifier");
    });

    it("shows 'Modifier la date' when planned with a date", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...partnershipItem, publication_date: "2026-06-01T00:00:00" },
          status: "planned",
        },
      });
      expect(wrapper.text()).toContain("Modifier la date");
    });

    it("shows 'Reprogrammer' when done", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...partnershipItem, publication_date: "2026-01-01T00:00:00" },
          status: "done",
        },
      });
      expect(wrapper.text()).toContain("Reprogrammer");
    });
  });

  describe("Actions for partnership-linked entries", () => {
    it("shows schedule and upload buttons", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: partnershipItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Planifier");
      expect(wrapper.text()).toContain("Ajouter");
    });

    it("does not show edit or delete buttons", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: partnershipItem, status: "unplanned" },
      });
      expect(wrapper.text()).not.toContain("Modifier");
      expect(wrapper.text()).not.toContain("Supprimer");
    });

    it("emits schedule event when schedule button clicked", async () => {
      const item = { ...partnershipItem, publication_date: "2026-01-01T00:00:00" };
      const wrapper = mount(CommunicationCard, {
        props: { item, status: "done" },
      });
      const buttons = wrapper.findAll("button");
      const scheduleButton = buttons.find((b) => b.text().includes("Reprogrammer"));
      await scheduleButton?.trigger("click");
      expect(wrapper.emitted("schedule")).toBeTruthy();
      expect(wrapper.emitted("schedule")?.[0]).toEqual([item]);
    });
  });

  describe("Actions for standalone entries", () => {
    it("shows Modifier and Supprimer buttons", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: standaloneItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Modifier");
      expect(wrapper.text()).toContain("Supprimer");
    });

    it("does not show schedule or upload buttons", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: standaloneItem, status: "unplanned" },
      });
      expect(wrapper.text()).not.toContain("Planifier");
      expect(wrapper.text()).not.toContain("Ajouter");
    });

    it("emits edit event when Modifier button clicked", async () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: standaloneItem, status: "unplanned" },
      });
      const buttons = wrapper.findAll("button");
      const editButton = buttons.find((b) => b.text().includes("Modifier"));
      await editButton?.trigger("click");
      expect(wrapper.emitted("edit")).toBeTruthy();
      expect(wrapper.emitted("edit")?.[0]).toEqual([standaloneItem]);
    });

    it("emits delete event when Supprimer button clicked", async () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: standaloneItem, status: "unplanned" },
      });
      const buttons = wrapper.findAll("button");
      const deleteButton = buttons.find((b) => b.text().includes("Supprimer"));
      await deleteButton?.trigger("click");
      expect(wrapper.emitted("delete")).toBeTruthy();
      expect(wrapper.emitted("delete")?.[0]).toEqual([standaloneItem]);
    });
  });

  describe("Status badge", () => {
    it("shows 'Terminée' badge for done status", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...partnershipItem, publication_date: "2026-01-01T00:00:00" },
          status: "done",
        },
      });
      expect(wrapper.text()).toContain("Terminée");
    });

    it("shows 'Planifiée' badge for planned status", () => {
      const wrapper = mount(CommunicationCard, {
        props: {
          item: { ...partnershipItem, publication_date: "2026-06-01T00:00:00" },
          status: "planned",
        },
      });
      expect(wrapper.text()).toContain("Planifiée");
    });

    it("shows 'Non planifiée' badge for unplanned status", () => {
      const wrapper = mount(CommunicationCard, {
        props: { item: partnershipItem, status: "unplanned" },
      });
      expect(wrapper.text()).toContain("Non planifiée");
    });
  });
});
