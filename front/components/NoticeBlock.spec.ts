import { describe, it, expect, vi } from "vitest";
import { mountSuspended } from "@nuxt/test-utils/runtime";
import { h } from "vue";
import NoticeBlock from "./NoticeBlock.vue";

describe("NoticeBlock", () => {
  describe("Basic rendering", () => {
    it("renders the title correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Test Title",
        },
      });

      expect(wrapper.find("h3").text()).toBe("Test Title");
    });

    it("renders slot content correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Test Title",
        },
        slots: {
          default: () => h("p", "Slot content here"),
        },
      });

      expect(wrapper.text()).toContain("Slot content here");
    });

    it("renders complex slot content with links", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Complex Content",
        },
        slots: {
          default: () => [
            h("p", { class: "mb-3" }, "Some intro text"),
            h("ul", { class: "list-disc" }, [h("li", "Item 1"), h("li", "Item 2")]),
          ],
        },
      });

      expect(wrapper.find("p.mb-3").exists()).toBe(true);
      expect(wrapper.find("ul.list-disc").exists()).toBe(true);
      expect(wrapper.findAll("li")).toHaveLength(2);
    });
  });

  describe("Variants", () => {
    it("renders info variant by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Info Notice",
        },
      });

      expect(wrapper.find(".bg-blue-50").exists()).toBe(true);
      expect(wrapper.find(".border-blue-200").exists()).toBe(true);
    });

    it("renders success variant correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Success Notice",
          variant: "success",
        },
      });

      expect(wrapper.find(".bg-green-50").exists()).toBe(true);
      expect(wrapper.find(".border-green-200").exists()).toBe(true);
    });

    it("renders warning variant correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Warning Notice",
          variant: "warning",
        },
      });

      expect(wrapper.find(".bg-amber-50").exists()).toBe(true);
      expect(wrapper.find(".border-amber-200").exists()).toBe(true);
    });

    it("renders error variant correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Error Notice",
          variant: "error",
        },
      });

      expect(wrapper.find(".bg-red-50").exists()).toBe(true);
      expect(wrapper.find(".border-red-200").exists()).toBe(true);
    });
  });

  describe("Icon", () => {
    it("does not render icon by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "No Icon",
        },
      });

      expect(wrapper.find("i.i-heroicons-information-circle").exists()).toBe(false);
    });

    it("renders icon when provided", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "With Icon",
          icon: "i-heroicons-information-circle",
        },
      });

      expect(wrapper.find("i.i-heroicons-information-circle").exists()).toBe(true);
    });

    it("icon has aria-hidden attribute", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "With Icon",
          icon: "i-heroicons-check-circle",
        },
      });

      const icon = wrapper.find("i.i-heroicons-check-circle");
      expect(icon.attributes("aria-hidden")).toBe("true");
    });
  });

  describe("Dismissible", () => {
    it("does not show dismiss button by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Not Dismissible",
        },
      });

      expect(wrapper.find("button[aria-label]").exists()).toBe(false);
    });

    it("shows dismiss button when dismissible is true", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Dismissible",
          dismissible: true,
        },
      });

      const dismissButton = wrapper.find("button[aria-label='Fermer']");
      expect(dismissButton.exists()).toBe(true);
    });

    it("uses custom dismiss label", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Dismissible",
          dismissible: true,
          dismissLabel: "Close this",
        },
      });

      const dismissButton = wrapper.find("button[aria-label='Close this']");
      expect(dismissButton.exists()).toBe(true);
    });

    it("hides component when dismiss button is clicked", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Dismissible",
          dismissible: true,
        },
      });

      const dismissButton = wrapper.find("button[aria-label='Fermer']");
      await dismissButton.trigger("click");

      // Le composant devrait être caché après dismiss
      expect(wrapper.find(".bg-blue-50").exists()).toBe(false);
    });

    it("emits dismiss event when dismissed", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Dismissible",
          dismissible: true,
        },
      });

      const dismissButton = wrapper.find("button[aria-label='Fermer']");
      await dismissButton.trigger("click");

      expect(wrapper.emitted("dismiss")).toBeTruthy();
    });
  });

  describe("Collapsible", () => {
    it("is not collapsible by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Not Collapsible",
        },
        slots: {
          default: () => h("p", "Content"),
        },
      });

      // Le titre n'est pas dans un bouton
      expect(wrapper.find("button[aria-expanded]").exists()).toBe(false);
      // Le contenu est visible
      expect(wrapper.text()).toContain("Content");
    });

    it("shows collapse button when collapsible is true", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Collapsible",
          collapsible: true,
        },
      });

      const collapseButton = wrapper.find("button[aria-expanded]");
      expect(collapseButton.exists()).toBe(true);
    });

    it("toggles content visibility on click", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Collapsible",
          collapsible: true,
        },
        slots: {
          default: () => h("p", { class: "test-content" }, "Visible content"),
        },
      });

      const collapseButton = wrapper.find("button[aria-expanded]");

      // Initialement ouvert
      expect(collapseButton.attributes("aria-expanded")).toBe("true");

      // Cliquer pour fermer
      await collapseButton.trigger("click");
      expect(collapseButton.attributes("aria-expanded")).toBe("false");

      // Cliquer pour rouvrir
      await collapseButton.trigger("click");
      expect(collapseButton.attributes("aria-expanded")).toBe("true");
    });

    it("respects defaultCollapsed prop", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Collapsible",
          collapsible: true,
          defaultCollapsed: true,
        },
      });

      const collapseButton = wrapper.find("button[aria-expanded]");
      expect(collapseButton.attributes("aria-expanded")).toBe("false");
    });
  });

  describe("Borderless", () => {
    it("has border by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "With Border",
        },
      });

      const outerDiv = wrapper.find("div");
      expect(outerDiv.classes()).toContain("border-t");
    });

    it("removes border when borderless is true", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Borderless",
          borderless: true,
        },
      });

      const outerDiv = wrapper.find("div");
      expect(outerDiv.classes()).not.toContain("border-t");
    });
  });

  describe("Size variants", () => {
    it("uses medium size by default", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Medium Size",
        },
      });

      expect(wrapper.find(".p-4").exists()).toBe(true);
      expect(wrapper.find("h3.text-sm").exists()).toBe(true);
    });

    it("renders small size correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Small Size",
          size: "sm",
        },
      });

      expect(wrapper.find(".p-3").exists()).toBe(true);
      expect(wrapper.find("h3.text-xs").exists()).toBe(true);
    });

    it("renders large size correctly", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Large Size",
          size: "lg",
        },
      });

      expect(wrapper.find(".p-5").exists()).toBe(true);
      expect(wrapper.find("h3.text-base").exists()).toBe(true);
    });
  });

  describe("Accessibility", () => {
    it("has role=status for non-error variants", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Info Notice",
          variant: "info",
        },
      });

      expect(wrapper.find("[role='status']").exists()).toBe(true);
    });

    it("has role=alert for error variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Error Notice",
          variant: "error",
        },
      });

      expect(wrapper.find("[role='alert']").exists()).toBe(true);
    });

    it("has aria-live=polite for non-error variants", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Info Notice",
          variant: "info",
        },
      });

      expect(wrapper.find("[aria-live='polite']").exists()).toBe(true);
    });

    it("has aria-live=assertive for error variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Error Notice",
          variant: "error",
        },
      });

      expect(wrapper.find("[aria-live='assertive']").exists()).toBe(true);
    });
  });

  describe("Actions slot", () => {
    it("does not render actions area when slot is empty", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "No Actions",
        },
      });

      expect(wrapper.findAll(".mt-3.flex.items-center.gap-2")).toHaveLength(0);
    });

    it("renders actions slot content", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "With Actions",
        },
        slots: {
          default: () => h("p", "Content"),
          actions: () => h("button", { class: "action-btn" }, "Click me"),
        },
      });

      expect(wrapper.find(".action-btn").exists()).toBe(true);
      expect(wrapper.find(".action-btn").text()).toBe("Click me");
    });
  });

  describe("Exposed methods", () => {
    it("exposes dismiss method", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Test",
        },
      });

      const vm = wrapper.vm as any;
      expect(typeof vm.dismiss).toBe("function");
    });

    it("exposes collapsed ref", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Test",
          collapsible: true,
        },
      });

      const vm = wrapper.vm as any;
      expect(vm.collapsed).toBe(false);
    });

    it("exposes dismissed ref", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Test",
        },
      });

      const vm = wrapper.vm as any;
      expect(vm.dismissed).toBe(false);
    });
  });

  describe("Snapshots", () => {
    it("matches snapshot for info variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Info Notice",
          variant: "info",
          icon: "i-heroicons-information-circle",
        },
        slots: {
          default: () => h("p", "This is an info message"),
        },
      });

      expect(wrapper.html()).toMatchSnapshot();
    });

    it("matches snapshot for success variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Success Notice",
          variant: "success",
          icon: "i-heroicons-check-circle",
        },
        slots: {
          default: () => h("p", "Operation completed successfully"),
        },
      });

      expect(wrapper.html()).toMatchSnapshot();
    });

    it("matches snapshot for warning variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Warning Notice",
          variant: "warning",
          icon: "i-heroicons-exclamation-triangle",
        },
        slots: {
          default: () => h("p", "Please be careful"),
        },
      });

      expect(wrapper.html()).toMatchSnapshot();
    });

    it("matches snapshot for error variant", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Error Notice",
          variant: "error",
          icon: "i-heroicons-x-circle",
        },
        slots: {
          default: () => h("p", "An error occurred"),
        },
      });

      expect(wrapper.html()).toMatchSnapshot();
    });

    it("matches snapshot with all features enabled", async () => {
      const wrapper = await mountSuspended(NoticeBlock, {
        props: {
          title: "Full Featured Notice",
          variant: "info",
          icon: "i-heroicons-information-circle",
          dismissible: true,
          collapsible: true,
          size: "lg",
        },
        slots: {
          default: () => h("p", "Full featured content"),
          actions: () => h("button", "Action"),
        },
      });

      expect(wrapper.html()).toMatchSnapshot();
    });
  });
});
