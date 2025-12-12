import { describe, it, expect, beforeEach } from "vitest";
import { mount } from "@vue/test-utils";
import SiretInput from "~/components/SiretInput.vue";
import { VALIDATION_MESSAGES } from "~/constants/validation";

describe("SiretInput", () => {
  let wrapper: any;

  beforeEach(() => {
    wrapper = mount(SiretInput, {
      props: {
        modelValue: "",
      },
    });
  });

  describe("Format validation", () => {
    it("should accept valid SIRET number (14 digits)", async () => {
      const input = wrapper.find("input");
      await input.setValue("12345678901234");
      await input.trigger("blur");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["12345678901234"]);
      expect(wrapper.emitted("validation")?.[1]).toEqual([true]);
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.SIRET_INVALID);
    });

    it("should reject SIRET with less than 14 digits", async () => {
      const input = wrapper.find("input");
      await input.setValue("123456789");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.SIRET_INVALID);
      expect(wrapper.emitted("validation")?.[1]).toEqual([false]);
    });

    it("should reject SIRET with more than 14 digits", async () => {
      const input = wrapper.find("input");
      // Le composant limite automatiquement à 14 chiffres
      await input.setValue("123456789012345678");
      await input.trigger("blur");

      // Vérifie que seulement 14 chiffres sont gardés
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["12345678901234"]);
    });

    it("should reject SIRET with non-numeric characters", async () => {
      const input = wrapper.find("input");
      await input.setValue("1234ABC5678901");

      // Les caractères non-numériques sont automatiquement supprimés
      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["12345678901"]);
    });

    it("should display error message on blur when invalid", async () => {
      const input = wrapper.find("input");
      await input.setValue("123");

      // Pas d'erreur pendant la saisie
      expect(wrapper.text()).not.toContain(VALIDATION_MESSAGES.SIRET_INVALID);

      await input.trigger("blur");

      // Erreur affichée après le blur
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.SIRET_INVALID);
    });

    it("should show required error when field is required and empty", async () => {
      wrapper = mount(SiretInput, {
        props: {
          modelValue: "",
          required: true,
        },
      });

      const input = wrapper.find("input");
      await input.trigger("blur");

      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.REQUIRED);
      expect(wrapper.emitted("validation")?.[1]).toEqual([false]);
    });
  });

  describe("Input sanitization", () => {
    it("should remove all non-numeric characters", async () => {
      const input = wrapper.find("input");
      await input.setValue("12-34 56.78/90!12@34");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["12345678901234"]);
    });

    it("should limit input to 14 characters", async () => {
      const input = wrapper.find("input");
      await input.setValue("123456789012345678901234");

      expect(wrapper.emitted("update:modelValue")?.[0]).toEqual(["12345678901234"]);
    });
  });

  describe("Props", () => {
    it("should display custom label", () => {
      wrapper = mount(SiretInput, {
        props: {
          modelValue: "",
          label: "Numéro SIRET personnalisé",
        },
      });

      expect(wrapper.text()).toContain("Numéro SIRET personnalisé");
    });

    it("should display hint text when provided", () => {
      wrapper = mount(SiretInput, {
        props: {
          modelValue: "",
          hint: "Le SIRET doit contenir 14 chiffres",
        },
      });

      expect(wrapper.text()).toContain("Le SIRET doit contenir 14 chiffres");
    });

    it("should disable input when disabled prop is true", () => {
      wrapper = mount(SiretInput, {
        props: {
          modelValue: "",
          disabled: true,
        },
      });

      const input = wrapper.find("input");
      expect(input.attributes("disabled")).toBeDefined();
    });

    it("should show errors during input when showErrorOnInput is true", async () => {
      wrapper = mount(SiretInput, {
        props: {
          modelValue: "",
          showErrorOnInput: true,
        },
      });

      const input = wrapper.find("input");
      await input.setValue("123");

      // Avec showErrorOnInput: true, l'erreur s'affiche pendant la saisie
      expect(wrapper.text()).toContain(VALIDATION_MESSAGES.SIRET_INVALID);
    });
  });

  describe("Validation events", () => {
    it("should emit validation event with true for valid SIRET", async () => {
      const input = wrapper.find("input");
      await input.setValue("12345678901234");

      // Le dernier événement validation émis devrait être true
      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([true]);
    });

    it("should emit validation event with false for invalid SIRET", async () => {
      const input = wrapper.find("input");
      await input.setValue("123");

      // Le dernier événement validation émis devrait être false
      const validationEvents = wrapper.emitted("validation");
      const lastValidation = validationEvents?.[validationEvents.length - 1];
      expect(lastValidation).toEqual([false]);
    });
  });

  describe("Visual feedback", () => {
    it("should display error message in red", async () => {
      const input = wrapper.find("input");
      await input.setValue("123");
      await input.trigger("blur");

      const errorText = wrapper.find(".text-red-600");
      expect(errorText.exists()).toBe(true);
      expect(errorText.text()).toBe(VALIDATION_MESSAGES.SIRET_INVALID);
    });
  });
});
