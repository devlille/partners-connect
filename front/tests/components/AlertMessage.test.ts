import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import AlertMessage from "~/components/AlertMessage.vue";

describe("AlertMessage Component", () => {
  describe("Rendering", () => {
    it("should render error alert with message", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "An error occurred",
        },
      });

      expect(wrapper.text()).toContain("An error occurred");
      expect(wrapper.classes()).toContain("bg-red-50");
      expect(wrapper.classes()).toContain("border-red-200");
    });

    it("should render success alert with message", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "success",
          message: "Operation successful",
        },
      });

      expect(wrapper.text()).toContain("Operation successful");
      expect(wrapper.classes()).toContain("bg-green-50");
      expect(wrapper.classes()).toContain("border-green-200");
    });

    it("should render warning alert with message", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "warning",
          message: "Warning message",
        },
      });

      expect(wrapper.text()).toContain("Warning message");
      expect(wrapper.classes()).toContain("bg-yellow-50");
      expect(wrapper.classes()).toContain("border-yellow-200");
    });

    it("should render info alert with message", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "info",
          message: "Information message",
        },
      });

      expect(wrapper.text()).toContain("Information message");
      expect(wrapper.classes()).toContain("bg-blue-50");
      expect(wrapper.classes()).toContain("border-blue-200");
    });

    it("should not render when modelValue is false", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          modelValue: false,
        },
      });

      expect(wrapper.html()).toBe("<!--v-if-->");
    });

    it("should render when modelValue is true", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          modelValue: true,
        },
      });

      expect(wrapper.text()).toContain("Error");
    });
  });

  describe("Title and Message", () => {
    it("should render title when provided", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          title: "Error Title",
          message: "Error message",
        },
      });

      expect(wrapper.text()).toContain("Error Title");
      expect(wrapper.text()).toContain("Error message");
    });

    it("should render without title when not provided", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error message",
        },
      });

      expect(wrapper.text()).toContain("Error message");
      expect(wrapper.find("h3").exists()).toBe(false);
    });

    it("should render slot content instead of message prop", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "This should not appear",
        },
        slots: {
          default: "<p>Custom slot content</p>",
        },
      });

      expect(wrapper.text()).toContain("Custom slot content");
      expect(wrapper.text()).not.toContain("This should not appear");
    });
  });

  describe("Icons", () => {
    it("should show icon by default", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.find('[class*="i-heroicons"]').exists()).toBe(true);
    });

    it("should hide icon when showIcon is false", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          showIcon: false,
        },
      });

      expect(wrapper.find('[class*="i-heroicons"]').exists()).toBe(false);
    });

    it("should render correct icon for each type", () => {
      const types = [
        { type: "error", icon: "i-heroicons:exclamation-circle" },
        { type: "success", icon: "i-heroicons:check-circle" },
        { type: "warning", icon: "i-heroicons:exclamation-triangle" },
        { type: "info", icon: "i-heroicons:information-circle" },
      ] as const;

      types.forEach(({ type, icon }) => {
        const wrapper = mount(AlertMessage, {
          props: {
            type,
            message: "Test",
          },
        });

        // Vérifier que l'icône est présente dans le HTML
        const html = wrapper.html();
        expect(html).toContain(icon);
      });
    });
  });

  describe("Dismissible", () => {
    it("should not show close button by default", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.find("button").exists()).toBe(false);
    });

    it("should show close button when dismissible is true", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          dismissible: true,
        },
      });

      expect(wrapper.find("button").exists()).toBe(true);
    });

    it("should emit update:modelValue when close button is clicked", async () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          dismissible: true,
          modelValue: true,
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual([false]);
    });

    it("should emit dismiss event when close button is clicked", async () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          dismissible: true,
        },
      });

      await wrapper.find("button").trigger("click");

      expect(wrapper.emitted("dismiss")).toBeTruthy();
    });
  });

  describe("Accessibility", () => {
    it('should have role="alert"', () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.attributes("role")).toBe("alert");
    });

    it("should have aria-label on close button", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          dismissible: true,
        },
      });

      const button = wrapper.find("button");
      expect(button.attributes("aria-label")).toBe("Fermer");
    });
  });

  describe("CSS Classes", () => {
    it("should apply correct color classes for error type", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.classes()).toContain("bg-red-50");
      expect(wrapper.classes()).toContain("border-red-200");
      expect(wrapper.classes()).toContain("text-red-700");
    });

    it("should apply correct color classes for success type", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "success",
          message: "Success",
        },
      });

      expect(wrapper.classes()).toContain("bg-green-50");
      expect(wrapper.classes()).toContain("border-green-200");
      expect(wrapper.classes()).toContain("text-green-700");
    });

    it("should apply custom classes from prop", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
          class: "custom-class mb-4",
        },
      });

      expect(wrapper.classes()).toContain("custom-class");
      expect(wrapper.classes()).toContain("mb-4");
    });
  });

  describe("Default Props", () => {
    it("should default to error type", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          message: "Message",
        },
      });

      expect(wrapper.classes()).toContain("bg-red-50");
    });

    it("should default to visible (modelValue true)", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.isVisible()).toBe(true);
    });

    it("should default to showing icon", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.find('[class*="i-heroicons"]').exists()).toBe(true);
    });

    it("should default to not dismissible", () => {
      const wrapper = mount(AlertMessage, {
        props: {
          type: "error",
          message: "Error",
        },
      });

      expect(wrapper.find("button").exists()).toBe(false);
    });
  });
});
